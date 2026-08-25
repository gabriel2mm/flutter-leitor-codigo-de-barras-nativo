import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'plugin_example_platform_interface.dart';

class MethodChannelPluginExample extends PluginExamplePlatform {
  @visibleForTesting
  final methodChannel = const MethodChannel('plugin_example');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>(
      'getPlatformVersion',
    );
    return version;
  }

  @override
  Future<String?> scanBarcode() async {
    final result = await methodChannel.invokeMethod<String>(
      'scanBarcode',
    );
    return result;
  }
}
