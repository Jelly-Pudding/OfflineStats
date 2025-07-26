# OfflineStats Plugin

A comprehensive player statistics tracking plugin for Minecraft 1.21.8 servers. Specifically created for MinecraftOffline.net, this plugin tracks player statistics and provides milestone rewards through integration with other plugins.

## Features

- **Player Statistics Tracking**: Time played, first seen, last seen, kills, deaths, and chat messages
- **Milestone Rewards**: Configurable rewards for reaching playtime, kill, and death milestones
- **Multi-Plugin Integration**: Works with SimpleHome, SimpleLifesteal, SimpleVote, and DiscordRelay
- **Developer API**: Full API access for other plugins
- **Announcements**: In-game and Discord milestone announcements

## Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/firstseen [player]` | `offlinestats.firstseen` | Show when a player first joined |
| `/lastseen [player]` | `offlinestats.lastseen` | Show when a player was last online |
| `/timeplayed [player]` | `offlinestats.timeplayed` | Show total playtime |
| `/kills [player]` | `offlinestats.kills` | Show total kills |
| `/deaths [player]` | `offlinestats.deaths` | Show total deaths |
| `/chatter [player]` | `offlinestats.chatter` | Show total chat messages |
| `/offlinestats reload` | `offlinestats.admin` | Reload plugin configuration |

## API

### Setup Dependencies

1. Download the latest OfflineStats.jar and place it in a libs directory
2. Add this to your `build.gradle` file:

```gradle
dependencies {
    compileOnly files('libs/OfflineStats-1.0-SNAPSHOT.jar')
}
```

3. Add this to your `plugin.yml` file:

```yaml
depend: [OfflineStats]
# OR for optional dependency:
softdepend: [OfflineStats]
```

### Getting OfflineStats Instance

```java
import org.bukkit.Bukkit;
import com.jellypudding.offlineStats.OfflineStats;
import com.jellypudding.offlineStats.api.OfflineStatsAPI;

Plugin offlineStatsPlugin = Bukkit.getPluginManager().getPlugin("OfflineStats");
if (offlineStatsPlugin instanceof OfflineStats && offlineStatsPlugin.isEnabled()) {
    OfflineStats offlineStats = (OfflineStats) offlineStatsPlugin;
    OfflineStatsAPI api = offlineStats.getApi();
}
```

### Available API Methods

#### Basic Player Statistics

```java
// Get complete player statistics by name
PlayerStats stats = api.getPlayerStats("PlayerName");
if (stats != null) {
    UUID playerUUID = stats.getPlayerUuid();
    String username = stats.getUsername();
    long firstSeen = stats.getFirstSeen();
    long lastSeen = stats.getLastSeen();
    long timePlayed = stats.getTimePlayed();
    int kills = stats.getKills();
    int deaths = stats.getDeaths();
    int chatMessages = stats.getChatMessages();
    boolean isOnline = stats.isOnline();
}

// Get player statistics by UUID
PlayerStats stats = api.getPlayerStats(playerUUID);
```

#### Individual Statistics

```java
// Get specific statistics by player name
long firstSeen = api.getPlayerFirstSeen("PlayerName");
long lastSeen = api.getPlayerLastSeen("PlayerName");
long timePlayed = api.getPlayerTimePlayed("PlayerName");
int kills = api.getPlayerKills("PlayerName");
int deaths = api.getPlayerDeaths("PlayerName");
int chatMessages = api.getPlayerChatMessages("PlayerName");

// Get specific statistics by UUID
long firstSeen = api.getPlayerFirstSeen(playerUUID);
long lastSeen = api.getPlayerLastSeen(playerUUID);
long timePlayed = api.getPlayerTimePlayed(playerUUID);
int kills = api.getPlayerKills(playerUUID);
int deaths = api.getPlayerDeaths(playerUUID);
int chatMessages = api.getPlayerChatMessages(playerUUID);
```

#### Formatted Statistics

```java
// Get human-readable formatted statistics
String formatted = api.getFormattedStat("PlayerName", "firstseen");
// Returns: "PlayerName first joined on 2024-01-15T14:30:00Z"

String formatted = api.getFormattedStat("PlayerName", "lastseen");  
// Returns: "PlayerName was last seen on 2024-01-15T18:45:00Z"
// OR: "PlayerName is currently online."

String formatted = api.getFormattedStat("PlayerName", "timeplayed");
// Returns: "PlayerName has played for 25 hours and 30 minutes"

String formatted = api.getFormattedStat("PlayerName", "kills");
// Returns: "PlayerName has 150 kills." OR "PlayerName has 1 kill."

String formatted = api.getFormattedStat("PlayerName", "deaths");
// Returns: "PlayerName has died 75 times." OR "PlayerName has died 1 time."

String formatted = api.getFormattedStat("PlayerName", "chatter"); 
// Returns: "PlayerName has sent 1,250 chat messages." OR "PlayerName has sent 1 chat message."
```

### PlayerStats Object

The `PlayerStats` object provides these methods:

```java
public class PlayerStats {
    // Basic data
    public UUID getPlayerUuid()
    public String getUsername()
    public long getFirstSeen()      // Unix timestamp
    public long getLastSeen()       // Unix timestamp  
    public long getTimePlayed()     // Milliseconds
    public int getKills()
    public int getDeaths()
    public int getChatMessages()
    public boolean isOnline()
    
    // Formatted strings
    public String getFormattedFirstSeen()
    public String getFormattedLastSeen()
    public String getFormattedTimePlayed()
}
```

## Historical Data Import

1. **Run the parser on your server logs:**
   ```bash
   python log_parser.py /path/to/your/minecraft/logs/ --verbose
   ```

2. **Copy the generated database to your plugin folder:**
   ```bash
   cp offlinestats.db /path/to/your/server/plugins/OfflineStats/
   ```

3. **Restart your server** - The plugin will now have all historical player data.
