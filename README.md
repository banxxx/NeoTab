# NeoTab - Advanced TAB List Enhancement

[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-green.svg)](https://minecraft.net/)
[![Forge](https://img.shields.io/badge/Forge-47.4.20-orange.svg)](https://files.minecraftforge.net/)
[![Java](https://img.shields.io/badge/Java-17-blue.svg)](https://adoptium.net/)

NeoTab is a comprehensive TAB list enhancement mod that provides rich text rendering, custom themes, player titles, and extensive customization options for Minecraft servers and clients.

## ✨ Features

### 🎨 **Rich Text & Themes**
- **Rich Text Processing**: Support for color tags, gradients, bold, italic, and more
- **Custom Themes**: Multiple built-in themes with support for custom theme creation
- **Syntax Highlighting**: Advanced text formatting with tag syntax highlighting
- **Placeholder System**: Dynamic content with placeholder replacement

### 📋 **Advanced TAB Lists**
- **Multi-page Support**: Automatic pagination for large player lists
- **Keyboard Navigation**: TAB+Left/Right arrow keys for page navigation
- **Custom Layouts**: Flexible layout system with automatic scaling
- **Player Information**: Health, ping, online duration, and custom titles

### ⚙️ **Extensive Customization**
- **Server Configuration**: Admin-controlled server-wide settings
- **Personal Settings**: Individual player customization options
- **Permission System**: Granular control over what players can customize
- **Live Updates**: Real-time configuration changes without restart

### 🔌 **Developer API**
- **Title Provider API**: Easy integration for rank/title plugins
- **Event System**: Comprehensive events for third-party mods
- **Configuration API**: Programmatic access to all settings
- **Network API**: Custom packet handling for advanced features

## 🚀 Installation

### Requirements
- **Minecraft**: 1.20.1
- **Forge**: 47.4.20 or compatible
- **Java**: 17 or higher

### Steps
1. Download the latest release from [Releases](../../releases)
2. Place the JAR file in your `mods` folder
3. Start your Minecraft client or server
4. Configure using `/neotab config` (admin) or `/neotab customize` (player)

## 🎮 Usage

### Commands
- `/neotab config` - Open admin configuration (requires permission)
- `/neotab customize` - Open personal customization screen
- `/neotab reload` - Reload configuration from disk (admin only)

### Permissions
- `neotab.configure` - Access to admin configuration
- `neotab.customize` - Access to personal customization
- Individual permission nodes for specific customization options

### Configuration Files
- `serverconfig/neotab-common.toml` - Server-wide configuration
- `config/neotab/players/<uuid>.json` - Individual player settings

## 🛠️ For Developers

### Adding Title Support
```java
// Register a title provider
NeoTabAPI.registerTitleProvider(new TitleProvider() {
    @Override
    public String getTitle(ServerPlayer player) {
        return "<color #FFD700>Admin</color>";
    }
    
    @Override
    public String getProviderId() {
        return "mymod:admin_titles";
    }
    
    @Override
    public int getPriority() {
        return 100; // Higher = more priority
    }
});
```

### Using Events
```java
@SubscribeEvent
public static void onGetPlayerTitle(GetPlayerTitleEvent event) {
    ServerPlayer player = event.getPlayer();
    if (isVIP(player)) {
        event.setTitle("<gradient #FF0000,#00FF00>VIP</gradient>");
    }
}
```

### Rich Text Format
```
<color #FF0000>Red Text</color>
<gradient #FF0000,#00FF00>Rainbow Text</gradient>
<bold>Bold Text</bold>
<italic>Italic Text</italic>
```

## 📊 Performance

NeoTab is designed with performance in mind:
- **Intelligent Caching**: Titles and configurations are cached to reduce computation
- **Optimized Rendering**: Efficient TAB list rendering with minimal overhead
- **Network Optimization**: Smart packet sending with change detection
- **Memory Management**: Proper cleanup prevents memory leaks

## 🤝 Contributing

We welcome contributions! Please see our [Contributing Guidelines](CONTRIBUTING.md) for details.

### Development Setup
1. Clone the repository
2. Import into your IDE (IntelliJ IDEA recommended)
3. Run `./gradlew genEclipseRuns` or `./gradlew genIntellijRuns`
4. Use the generated run configurations

### Building
```bash
./gradlew build
```

## 📄 License

This project is licensed under [All Rights Reserved](LICENSE.txt).

## 🐛 Bug Reports & Feature Requests

Please use our [Issue Tracker](../../issues) to report bugs or request features.

### Before Reporting
- Check if the issue already exists
- Include your Minecraft, Forge, and NeoTab versions
- Provide detailed reproduction steps
- Include relevant log files

## 📞 Support

- **Discord**: [Join our Discord](https://discord.gg/example)
- **Issues**: [GitHub Issues](../../issues)
- **Wiki**: [Documentation Wiki](../../wiki)

## 🙏 Acknowledgments

- **Original NeoForge Version**: Thanks to the original developers
- **Minecraft Forge**: For the excellent modding framework
- **Community**: For testing and feedback

---

**Made with ❤️ for the Minecraft community**