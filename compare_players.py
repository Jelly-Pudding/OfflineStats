#!/usr/bin/env python3
"""
Compare players in database vs playerdata folder
"""

import sqlite3
import os
import argparse
from pathlib import Path

def get_database_uuids(db_path):
    """Get all UUIDs from the database"""
    conn = sqlite3.connect(db_path)
    cursor = conn.execute('SELECT uuid, username FROM players')
    db_players = {row[0]: row[1] for row in cursor.fetchall()}
    conn.close()
    return db_players

def get_playerdata_uuids(playerdata_path):
    """Get all UUIDs from playerdata folder"""
    playerdata_path = Path(playerdata_path)
    if not playerdata_path.exists():
        print(f"Error: Playerdata path {playerdata_path} does not exist")
        return set()
    
    # Get all .dat files and extract UUIDs (filenames without .dat extension)
    dat_files = list(playerdata_path.glob("*.dat"))
    uuids = {file.stem for file in dat_files}
    return uuids

def main():
    parser = argparse.ArgumentParser(description='Compare database players with playerdata folder')
    parser.add_argument('playerdata_path', help='Path to playerdata folder')
    parser.add_argument('--db', default='offlinestats.db', help='Database file (default: offlinestats.db)')
    parser.add_argument('--sample-size', type=int, default=10, help='Number of examples to show (default: 10)')
    
    args = parser.parse_args()
    
    print("Loading database players...")
    db_players = get_database_uuids(args.db)
    db_uuids = set(db_players.keys())
    
    print("Loading playerdata UUIDs...")
    playerdata_uuids = get_playerdata_uuids(args.playerdata_path)
    
    print(f"\n=== COMPARISON RESULTS ===")
    print(f"Database players: {len(db_uuids)}")
    print(f"Playerdata files: {len(playerdata_uuids)}")
    print(f"Difference: {len(playerdata_uuids) - len(db_uuids)}")
    
    # Find differences
    only_in_playerdata = playerdata_uuids - db_uuids
    only_in_database = db_uuids - playerdata_uuids
    in_both = db_uuids & playerdata_uuids
    
    print(f"\nOnly in playerdata: {len(only_in_playerdata)}")
    print(f"Only in database: {len(only_in_database)}")
    print(f"In both: {len(in_both)}")
    
    # Show examples
    if only_in_playerdata:
        print(f"\n=== EXAMPLES: In playerdata but NOT in database ===")
        sample_playerdata = list(only_in_playerdata)[:args.sample_size]
        for uuid in sample_playerdata:
            print(f"  {uuid}")
    
    if only_in_database:
        print(f"\n=== EXAMPLES: In database but NOT in playerdata ===")
        sample_database = list(only_in_database)[:args.sample_size]
        for uuid in sample_database:
            username = db_players.get(uuid, "Unknown")
            print(f"  {uuid} ({username})")
    
    # Show some examples of players that are in both
    print(f"\n=== EXAMPLES: In both (for verification) ===")
    sample_both = list(in_both)[:5]
    for uuid in sample_both:
        username = db_players.get(uuid, "Unknown")
        print(f"  {uuid} ({username})")

if __name__ == "__main__":
    main() 