#!/usr/bin/env python3
"""
Minecraft Server Log Parser for OfflineStats Plugin
Parses server logs and populates the players table with historical data.
"""

import sqlite3
import re
import gzip
from datetime import datetime, timezone
from pathlib import Path
import argparse
import logging

logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

class OfflineStatsParser:
    def __init__(self, db_path: str = "offlinestats.db"):
        self.db_path = db_path
        self.conn = None
        self.player_sessions = {}
        self.player_uuids = {}
        self.setup_database()
        self.compile_patterns()

    def compile_patterns(self):
        """Compile regex patterns for log parsing"""
        # Base log format
        self.log_pattern = re.compile(r'\[(\d{2}:\d{2}:\d{2})\] \[([^/]+)/([^]]+)\]: (.+)')

        # Player UUID lookup patterns
        self.uuid_pattern = re.compile(r'UUID of player (\.[\w]+|[\w]+) is ([\w-]+)')
        # Floodgate/Bedrock player UUID pattern
        # Bedrock players connecting through Geyser/Floodgate have usernames with "." prefix
        self.floodgate_uuid_pattern = re.compile(r'\[floodgate\] Floodgate player logged in as (\.[\w]+) joined \(UUID: ([\w-]+)\)')

        # Player login with coordinates
        self.login_pattern = re.compile(r'(\.[\w]+|[\w]+)\[/[^]]+\] logged in with entity id \d+')

        # Player join/leave
        self.join_pattern = re.compile(r'(\.[\w]+|[\w]+) joined the game')
        self.leave_pattern = re.compile(r'(\.[\w]+|[\w]+) left the game')

        # PvP deaths (for kill counting) - these award kills to the killer
        self.pvp_death_patterns = [
            re.compile(r'(\.[\w]+|[\w]+) was slain by (\.[\w]+|[\w]+)'),
            re.compile(r'(\.[\w]+|[\w]+) was shot by (\.[\w]+|[\w]+)'),
            re.compile(r'(\.[\w]+|[\w]+) was blown up by (\.[\w]+|[\w]+)'),
            re.compile(r'(\.[\w]+|[\w]+) was fireballed by (\.[\w]+|[\w]+)'),
            re.compile(r'(\.[\w]+|[\w]+) was shot by a skull from (\.[\w]+|[\w]+)'),
            re.compile(r'(\.[\w]+|[\w]+) was impaled by (\.[\w]+|[\w]+)'),
            re.compile(r'(\.[\w]+|[\w]+) was destroyed by (\.[\w]+|[\w]+)'),
            re.compile(r'(\.[\w]+|[\w]+) was pummeled by (\.[\w]+|[\w]+)'),
            re.compile(r'(\.[\w]+|[\w]+) was killed by (\.[\w]+|[\w]+) using magic'),
            re.compile(r'(\.[\w]+|[\w]+) was frozen to death by (\.[\w]+|[\w]+)'),
            re.compile(r'(\.[\w]+|[\w]+) was squashed by (\.[\w]+|[\w]+)'),
        ]

        # All death patterns (comprehensive list from Minecraft documentation)
        self.death_patterns = [
            # Cactus
            re.compile(r'(\.[\w]+|[\w]+) was pricked to death'),
            # Drowning
            re.compile(r'(\.[\w]+|[\w]+) drowned'),
            re.compile(r'(\.[\w]+|[\w]+) died from dehydration'),
            # Elytra
            re.compile(r'(\.[\w]+|[\w]+) experienced kinetic energy'),
            # Explosions
            re.compile(r'(\.[\w]+|[\w]+) blew up'),
            re.compile(r'(\.[\w]+|[\w]+) was blown up by'),
            re.compile(r'(\.[\w]+|[\w]+) was killed by \[Intentional Game Design\]'),
            # Falling
            re.compile(r'(\.[\w]+|[\w]+) hit the ground too hard'),
            re.compile(r'(\.[\w]+|[\w]+) fell from a high place'),
            re.compile(r'(\.[\w]+|[\w]+) fell off a ladder'),
            re.compile(r'(\.[\w]+|[\w]+) fell off some vines'),
            re.compile(r'(\.[\w]+|[\w]+) fell off some weeping vines'),
            re.compile(r'(\.[\w]+|[\w]+) fell off some twisting vines'),
            re.compile(r'(\.[\w]+|[\w]+) fell off scaffolding'),
            re.compile(r'(\.[\w]+|[\w]+) fell while climbing'),
            re.compile(r'(\.[\w]+|[\w]+) was doomed to fall'),
            re.compile(r'(\.[\w]+|[\w]+) was impaled on a stalagmite'),
            # Falling blocks
            re.compile(r'(\.[\w]+|[\w]+) was squashed by a falling anvil'),
            re.compile(r'(\.[\w]+|[\w]+) was squashed by a falling block'),
            re.compile(r'(\.[\w]+|[\w]+) was skewered by a falling stalactite'),
            # Fire
            re.compile(r'(\.[\w]+|[\w]+) went up in flames'),
            re.compile(r'(\.[\w]+|[\w]+) burned to death'),
            re.compile(r'(\.[\w]+|[\w]+) was burned to a crisp'),
            # Firework
            re.compile(r'(\.[\w]+|[\w]+) went off with a bang'),
            # Lava
            re.compile(r'(\.[\w]+|[\w]+) tried to swim in lava'),
            # Lightning
            re.compile(r'(\.[\w]+|[\w]+) was struck by lightning'),
            # Magma
            re.compile(r'(\.[\w]+|[\w]+) discovered the floor was lava'),
            re.compile(r'(\.[\w]+|[\w]+) walked into the danger zone'),
            # Magic
            re.compile(r'(\.[\w]+|[\w]+) was killed by magic'),
            # Powder Snow
            re.compile(r'(\.[\w]+|[\w]+) froze to death'),
            re.compile(r'(\.[\w]+|[\w]+) was frozen to death'),
            # Players/mobs
            re.compile(r'(\.[\w]+|[\w]+) was slain by'),
            re.compile(r'(\.[\w]+|[\w]+) was stung to death'),
            re.compile(r'(\.[\w]+|[\w]+) was obliterated by a sonically-charged shriek'),
            # Projectiles
            re.compile(r'(\.[\w]+|[\w]+) was shot by'),
            re.compile(r'(\.[\w]+|[\w]+) was pummeled by'),
            re.compile(r'(\.[\w]+|[\w]+) was fireballed by'),
            re.compile(r'(\.[\w]+|[\w]+) was shot by a skull from'),
            # Starving
            re.compile(r'(\.[\w]+|[\w]+) starved to death'),
            # Suffocation
            re.compile(r'(\.[\w]+|[\w]+) suffocated in a wall'),
            re.compile(r'(\.[\w]+|[\w]+) was squished too much'),
            re.compile(r'(\.[\w]+|[\w]+) was squashed by'),
            re.compile(r'(\.[\w]+|[\w]+) left the confines of this world'),
            # Sweet Berry
            re.compile(r'(\.[\w]+|[\w]+) was poked to death by a sweet berry bush'),
            # Thorns
            re.compile(r'(\.[\w]+|[\w]+) was killed while trying to hurt'),
            # Trident
            re.compile(r'(\.[\w]+|[\w]+) was impaled by'),
            # Mace
            re.compile(r'(\.[\w]+|[\w]+) was destroyed by'),
            # Void
            re.compile(r'(\.[\w]+|[\w]+) fell out of the world'),
            re.compile(r'(\.[\w]+|[\w]+) didn\'t want to live in the same world as'),
            # Wither
            re.compile(r'(\.[\w]+|[\w]+) withered away'),
            # Generic
            re.compile(r'(\.[\w]+|[\w]+) died'),
            re.compile(r'(\.[\w]+|[\w]+) was killed'),
        ]

        # Chat messages
        self.chat_patterns = [
            re.compile(r'<(\.[\w]+|[\w]+)> '),
            re.compile(r'\[Not Secure\] <(\.[\w]+|[\w]+)> '),
        ]

    def setup_database(self):
        """Initialise SQLite database with the exact same schema as the plugin"""
        self.conn = sqlite3.connect(self.db_path)

        # Create the exact same table as the plugin
        self.conn.execute('''
            CREATE TABLE IF NOT EXISTS players (
                uuid TEXT PRIMARY KEY,
                username TEXT NOT NULL,
                first_seen DATETIME NOT NULL,
                last_seen DATETIME NOT NULL,
                time_played BIGINT DEFAULT 0,
                session_start BIGINT DEFAULT 0,
                kills INTEGER DEFAULT 0,
                deaths INTEGER DEFAULT 0,
                chat_messages INTEGER DEFAULT 0
            )
        ''')

        self.conn.commit()
        logger.info(f"Database initialised: {self.db_path}")

    def parse_log_line(self, line: str, log_date: str):
        """Parse a single log line"""
        match = self.log_pattern.match(line.strip())
        if not match:
            return None

        time_str, thread, level, message = match.groups()

        try:
            timestamp = datetime.strptime(f"{log_date} {time_str}", "%Y-%m-%d %H:%M:%S")
            return timestamp, message
        except ValueError:
            return None

    def get_datetime_string(self, timestamp):
        """Convert timestamp to the datetime format used by the plugin"""
        return timestamp.strftime("%Y-%m-%d %H:%M:%S")

    def process_uuid_event(self, message, timestamp):
        """Process UUID lookup events"""
        match = self.uuid_pattern.search(message)
        if match:
            username, uuid = match.groups()
            self.player_uuids[username] = uuid

            datetime_str = self.get_datetime_string(timestamp)

            # Check if this UUID already exists
            cursor = self.conn.execute('SELECT username FROM players WHERE uuid = ?', (uuid,))
            existing = cursor.fetchone()

            if existing:
                # Update existing player (might be username change)
                self.conn.execute('''
                    UPDATE players SET username = ?, last_seen = ? WHERE uuid = ?
                ''', (username, datetime_str, uuid))
                logger.debug(f"Updated existing UUID: {username} -> {uuid}")
            else:
                # Check if this username exists with a different UUID (shouldn't happen but let's be safe)
                cursor = self.conn.execute('SELECT uuid FROM players WHERE username = ?', (username,))
                existing_uuid = cursor.fetchone()

                if existing_uuid:
                    logger.warning(f"Player {username} already has UUID {existing_uuid[0]}, but now has {uuid}. This might indicate a duplicate or UUID change.")

                # Create new player
                self.conn.execute('''
                    INSERT INTO players (uuid, username, first_seen, last_seen, time_played, session_start, kills, deaths, chat_messages)
                    VALUES (?, ?, ?, ?, 0, 0, 0, 0, 0)
                ''', (uuid, username, datetime_str, datetime_str))
                logger.debug(f"UUID mapping: {username} -> {uuid}")

    def process_floodgate_uuid_event(self, message, timestamp):
        """Process Floodgate/Bedrock player UUID events"""
        match = self.floodgate_uuid_pattern.search(message)
        if match:
            username, uuid = match.groups()
            self.player_uuids[username] = uuid

            datetime_str = self.get_datetime_string(timestamp)

            # Check if this UUID already exists
            cursor = self.conn.execute('SELECT username FROM players WHERE uuid = ?', (uuid,))
            existing = cursor.fetchone()

            if existing:
                # Update existing player (might be username change)
                self.conn.execute('''
                    UPDATE players SET username = ?, last_seen = ? WHERE uuid = ?
                ''', (username, datetime_str, uuid))
                logger.debug(f"Updated existing Bedrock UUID: {username} -> {uuid}")
            else:
                # Create new Bedrock player
                self.conn.execute('''
                    INSERT INTO players (uuid, username, first_seen, last_seen, time_played, session_start, kills, deaths, chat_messages)
                    VALUES (?, ?, ?, ?, 0, 0, 0, 0, 0)
                ''', (uuid, username, datetime_str, datetime_str))
                logger.debug(f"Bedrock UUID mapping: {username} -> {uuid}")

    def process_join_event(self, message, timestamp):
        """Process player join events"""
        match = self.join_pattern.search(message)
        if match:
            username = match.group(1)
            if username in self.player_uuids:
                uuid = self.player_uuids[username]
                timestamp_ms = int(timestamp.timestamp() * 1000)

                # Start session tracking
                self.player_sessions[username] = {
                    'uuid': uuid,
                    'start_time': timestamp_ms
                }

                # Update last_seen
                datetime_str = self.get_datetime_string(timestamp)
                self.conn.execute('''
                    UPDATE players SET last_seen = ? WHERE uuid = ?
                ''', (datetime_str, uuid))

                logger.debug(f"Join: {username}")

    def process_leave_event(self, message, timestamp):
        """Process player leave events"""
        match = self.leave_pattern.search(message)
        if match:
            username = match.group(1)
            if username in self.player_sessions:
                session = self.player_sessions[username]
                timestamp_ms = int(timestamp.timestamp() * 1000)

                # Calculate session duration
                duration = timestamp_ms - session['start_time']

                # Update playtime and last_seen
                datetime_str = self.get_datetime_string(timestamp)
                self.conn.execute('''
                    UPDATE players SET 
                        time_played = time_played + ?,
                        last_seen = ?
                    WHERE uuid = ?
                ''', (duration, datetime_str, session['uuid']))

                del self.player_sessions[username]
                logger.debug(f"Leave: {username}, duration: {duration}ms")

    def process_death_event(self, message):
        """Process death events"""
        # Check for PvP kills first
        for pattern in self.pvp_death_patterns:
            match = pattern.search(message)
            if match:
                victim = match.group(1)
                killer = match.group(2)

                # Increment death count for victim
                if victim in self.player_uuids:
                    victim_uuid = self.player_uuids[victim]
                    self.conn.execute('UPDATE players SET deaths = deaths + 1 WHERE uuid = ?', (victim_uuid,))

                # Increment kill count for killer
                if killer in self.player_uuids:
                    killer_uuid = self.player_uuids[killer]
                    self.conn.execute('UPDATE players SET kills = kills + 1 WHERE uuid = ?', (killer_uuid,))

                logger.debug(f"PvP: {killer} killed {victim}")
                return

        # Check for any death (PvE)
        for pattern in self.death_patterns:
            match = pattern.search(message)
            if match:
                victim = match.group(1)
                if victim in self.player_uuids:
                    victim_uuid = self.player_uuids[victim]
                    self.conn.execute('UPDATE players SET deaths = deaths + 1 WHERE uuid = ?', (victim_uuid,))
                    logger.debug(f"Death: {victim}")
                return

    def process_chat_event(self, message):
        """Process chat message events"""
        for pattern in self.chat_patterns:
            match = pattern.search(message)
            if match:
                username = match.group(1)
                if username in self.player_uuids:
                    uuid = self.player_uuids[username]
                    self.conn.execute('UPDATE players SET chat_messages = chat_messages + 1 WHERE uuid = ?', (uuid,))
                    logger.debug(f"Chat: {username}")
                return

    def process_log_file(self, file_path):
        """Process a single log file"""
        logger.info(f"Processing: {file_path}")

        # Extract date from filename
        date_match = re.search(r'(\d{4}-\d{2}-\d{2})', file_path.name)
        if date_match:
            log_date = date_match.group(1)
        else:
            # Use file modification time as fallback
            mtime = datetime.fromtimestamp(file_path.stat().st_mtime)
            log_date = mtime.strftime("%Y-%m-%d")

        # Open file (handle both regular and gzipped files)
        opener = gzip.open if file_path.suffix == '.gz' else open

        processed_count = 0

        try:
            with opener(file_path, 'rt', encoding='utf-8', errors='ignore') as f:
                for line in f:
                    parsed = self.parse_log_line(line, log_date)
                    if not parsed:
                        continue

                    timestamp, message = parsed
                    processed_count += 1

                    # Process different event types
                    if 'UUID of player' in message:
                        self.process_uuid_event(message, timestamp)
                    elif '[floodgate] Floodgate player logged in as' in message:
                        self.process_floodgate_uuid_event(message, timestamp)
                    elif 'joined the game' in message:
                        self.process_join_event(message, timestamp)
                    elif 'left the game' in message:
                        self.process_leave_event(message, timestamp)
                    elif any(death_word in message for death_word in [
                        'was slain', 'was shot', 'fell from', 'drowned', 'was blown up', ' died',
                        'was pricked to death', 'died from dehydration', 'experienced kinetic energy',
                        'blew up', 'was killed by [Intentional Game Design]', 'hit the ground too hard',
                        'fell off', 'fell while climbing', 'was doomed to fall', 'was impaled on a stalagmite',
                        'was squashed by', 'was skewered by', 'went up in flames', 'burned to death',
                        'was burned to a crisp', 'went off with a bang', 'tried to swim in lava',
                        'was struck by lightning', 'discovered the floor was lava', 'walked into the danger zone',
                        'was killed by magic', 'froze to death', 'was frozen to death', 'was stung to death',
                        'was obliterated by', 'was pummeled by', 'was fireballed by', 'was shot by a skull',
                        'starved to death', 'suffocated in a wall', 'was squished too much',
                        'left the confines of this world', 'was poked to death', 'was killed while trying to hurt',
                        'was impaled by', 'was destroyed by', 'fell out of the world', 'didn\'t want to live',
                        'withered away', 'was killed'
                    ]):
                        self.process_death_event(message)
                    elif any(pattern.search(message) for pattern in self.chat_patterns):
                        self.process_chat_event(message)

                    # Commit periodically
                    if processed_count % 1000 == 0:
                        self.conn.commit()

        except Exception as e:
            logger.error(f"Error processing {file_path}: {e}")

        self.conn.commit()
        logger.info(f"Processed {processed_count} lines from {file_path}")

    def process_log_directory(self, log_dir):
        """Process all log files in a directory"""
        log_path = Path(log_dir)
        if not log_path.exists():
            logger.error(f"Log directory not found: {log_dir}")
            return

        # Find all log files
        log_files = list(log_path.glob("*.log")) + list(log_path.glob("*.log.gz"))
        log_files.sort()

        logger.info(f"Found {len(log_files)} log files")

        for log_file in log_files:
            self.process_log_file(log_file)

    def generate_report(self):
        """Generate a simple statistics report"""
        cursor = self.conn.execute('''
            SELECT 
                COUNT(*) as total_players,
                SUM(time_played) as total_playtime_ms,
                SUM(kills) as total_kills,
                SUM(deaths) as total_deaths,
                SUM(chat_messages) as total_chat_messages
            FROM players
        ''')
        stats = cursor.fetchone()

        total_hours = stats[1] / (1000 * 60 * 60) if stats[1] else 0

        print("\n" + "="*50)
        print("OFFLINESTATS DATABASE REPORT")
        print("="*50)
        print(f"Total Players: {stats[0]:,}")
        print(f"Total Playtime: {total_hours:.1f} hours")
        print(f"Total Kills: {stats[2]:,}")
        print(f"Total Deaths: {stats[3]:,}")
        print(f"Total Chat Messages: {stats[4]:,}")

        # Top players by playtime
        print("\nTOP 10 PLAYERS BY PLAYTIME:")
        cursor = self.conn.execute('''
            SELECT username, time_played, kills, deaths, chat_messages
            FROM players
            ORDER BY time_played DESC
            LIMIT 10
        ''')

        for i, (username, playtime_ms, kills, deaths, messages) in enumerate(cursor.fetchall(), 1):
            hours = playtime_ms / (1000 * 60 * 60) if playtime_ms else 0
            print(f"{i:2d}. {username:<20} {hours:>8.1f}h  K:{kills:>4d}  D:{deaths:>4d}  M:{messages:>5d}")

        print("="*50)

    def close(self):
        """Close database connection"""
        if self.conn:
            self.conn.close()

def main():
    parser = argparse.ArgumentParser(description='Parse Minecraft logs for OfflineStats plugin')
    parser.add_argument('log_directory', help='Directory containing log files')
    parser.add_argument('--database', '-d', default='offlinestats.db', help='Database file path')
    parser.add_argument('--verbose', '-v', action='store_true', help='Enable verbose logging')

    args = parser.parse_args()

    if args.verbose:
        logging.getLogger().setLevel(logging.DEBUG)

    # Create parser and process logs
    parser = OfflineStatsParser(args.database)

    try:
        parser.process_log_directory(args.log_directory)
        parser.generate_report()
    finally:
        parser.close()

    logger.info(f"Database created: {args.database}")
    logger.info("You can now copy this database to your plugin's data folder")

if __name__ == "__main__":
    main() 