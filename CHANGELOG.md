# NeoTab Changelog

## Version 1.0.0 - Initial Forge 1.20.1 Port

### 🎉 **Major Features**
- **Complete NeoForge to Forge Port**: Successfully ported NeoTab from NeoForge 1.21.1 to Forge 1.20.1
- **Rich Text Processing**: Full support for rich text rendering with color tags, gradients, and formatting
- **Custom Themes**: Comprehensive theme system with support for custom themes and configurations
- **Multi-page TAB Lists**: Advanced pagination system with TAB+Arrow key navigation
- **Player Titles**: Complete API for third-party mods to provide player titles and ranks
- **Extensive Customization**: Both server-wide and per-player configuration options

### 🔧 **Core Systems**
- **Network System**: Complete network packet system rebuilt from NeoForge Payload to Forge SimpleChannel
- **Configuration Management**: Advanced configuration system with server and personal settings
- **Permission System**: Granular permission control for player customization options
- **Client State Management**: Sophisticated client-side state management with GUI scaling support
- **Event System**: Full event handling for server and client-side operations

### 🎨 **User Interface**
- **Configuration Screen**: Intuitive admin configuration interface
- **Customization Screen**: Player-friendly personal settings interface
- **Mixin Integration**: Advanced TAB list rendering modifications
- **Keyboard Shortcuts**: TAB+Left/Right arrow keys for page navigation

### 🌐 **API & Integration**
- **Public API**: Complete API for third-party mod integration
- **Title Provider System**: Interface for mods to provide player titles
- **Event System**: Comprehensive event system for extensibility
- **Command System**: Full command support with permission checking

### 📊 **Performance & Optimization**
- **Caching System**: Intelligent caching for titles, metrics, and configurations
- **Network Optimization**: Optimized packet sending with change detection
- **Memory Management**: Proper cleanup and memory leak prevention
- **Rendering Optimization**: Efficient TAB list rendering with pagination

### 🔧 **Technical Details**
- **Minecraft Version**: 1.20.1
- **Forge Version**: 47.4.20
- **Java Version**: 17
- **Mappings**: Parchment 2023.09.03-1.20.1

### 🛠 **API Adaptations**
- `RegistryFriendlyByteBuf` → `FriendlyByteBuf`
- `ResourceLocation.fromNamespaceAndPath()` → `new ResourceLocation()`
- `Math.clamp()` → `MathUtils.clamp()` (Java 17 compatibility)
- Event system adapted from NeoForge to Forge
- Network system completely rebuilt for Forge SimpleChannel

### 📝 **Localization**
- **English (US)**: Complete translation
- **Chinese (Simplified)**: Complete translation
- **Extensible**: Easy to add more languages

### 🧪 **Testing Status**
- ✅ **Compilation**: Successful build with no errors
- ✅ **Network System**: All packets implemented and tested
- ✅ **Core Logic**: All business logic ported and functional
- ✅ **Client UI**: Basic interfaces implemented
- ✅ **Mixin System**: TAB list modifications working
- ✅ **Resource Files**: All assets properly loaded

### 🎯 **Known Limitations**
- Some advanced UI features may need further refinement
- Extensive multiplayer testing recommended before production use
- Performance testing in large server environments pending

### 📦 **Installation**
1. Download the JAR file from releases
2. Place in your `mods` folder
3. Ensure you have Forge 47.4.20 or compatible version
4. Start your server/client

### 🔮 **Future Plans**
- Extended UI customization options
- Additional theme presets
- Performance optimizations
- More third-party integrations

---

**Note**: This is a complete port from NeoForge 1.21.1 to Forge 1.20.1. All core functionality has been preserved and adapted for the target platform.