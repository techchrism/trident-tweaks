This plugin adds configuration to change various mechanics of tridents. Designed for Bukkit/Spigot/Paper 26.2+

Config:
```yaml
# Make the impaling enchantment act as it does on bedrock edition: deals extra damage to mobs touching water or rain
enable-bedrock-impaling: true

# If a trident enchanted with loyalty is heading into the void (such as in the end), this will save it and make it return
enable-void-saving: true

# If a trident is thrown from the offhand, it will try to go back to the offhand when it's picked up
enable-offhand-return: true

# Disables tridents enchanted with loyalty from going through portals
# (which makes the trident not return until the player goes through the portal)
disable-loyalty-portals: true

# Makes drowned mobs drop tridents as often as they do in bedrock edition (25% of the time when holding a trident)
enable-bedrock-dropping: true

# Enables the use of channeling tridents on lightning rods in rainy weather as well as thunderous weather
# This is how it works on Bedrock edition
enable-rain-lightning-rod: true
```