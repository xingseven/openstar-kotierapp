import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'app.dart';
import 'core/easy_tier_service.dart';
import 'locator_stub.dart' if (dart.library.ffi) 'locator_ffi.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  GoogleFonts.config.allowRuntimeFetching = false;
  EasyTierServiceFactory.setInstance(getPlatformEasyTierService());
  runApp(const QtEasyTierApp());
}
