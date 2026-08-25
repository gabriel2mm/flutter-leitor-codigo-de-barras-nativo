import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'plugin_example_method_channel.dart';

abstract class PluginExamplePlatform extends PlatformInterface {
  PluginExamplePlatform() : super(token: _token);

  static final Object _token = Object();

  static PluginExamplePlatform _instance = MethodChannelPluginExample();
  static PluginExamplePlatform get instance => _instance;

  static set instance(PluginExamplePlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }

  Future<String?> scanBarcode() {
    throw UnimplementedError('scanBarcode() has not been implemented.');
  }
}
