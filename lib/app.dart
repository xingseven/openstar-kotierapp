import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'core/easy_tier_service.dart';
import 'core/log_service.dart';
import 'pages/home_page.dart';

class QtEasyTierApp extends StatefulWidget {
  const QtEasyTierApp({super.key});

  @override
  State<QtEasyTierApp> createState() => _QtEasyTierAppState();
}

class _QtEasyTierAppState extends State<QtEasyTierApp> {
  late Future<bool> _initFuture;

  @override
  void initState() {
    super.initState();
    _initFuture = _initializeService();
  }

  Future<bool> _initializeService() async {
    final service = EasyTierServiceFactory.instance;
    final initialized = await service.initialize();
    if (initialized) {
      return true;
    }

    if (service.platform == 'native') {
      LogService.instance.warn(
        '原生后端初始化失败，当前不再回退到兼容模式',
        source: 'App',
      );
      return false;
    }

    return false;
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'QtEasyTier',
      debugShowCheckedModeBanner: false,
      theme: _buildTheme(Brightness.light),
      darkTheme: _buildTheme(Brightness.dark),
      themeMode: ThemeMode.light,
      home: FutureBuilder<bool>(
        future: _initFuture,
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return const _SplashScreen();
          }
          if (snapshot.hasError || snapshot.data != true) {
            return Scaffold(
              backgroundColor: const Color(0xFF1a1a2e),
              body: Center(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    const Icon(Icons.error_outline, color: Colors.redAccent, size: 48),
                    const SizedBox(height: 16),
                    Text(
                      '初始化失败',
                      style: TextStyle(
                        color: Colors.grey[300],
                        fontSize: 18,
                      ),
                    ),
                    const SizedBox(height: 24),
                    ElevatedButton(
                      onPressed: () {
                        setState(() {
                          _initFuture = _initializeService();
                        });
                      },
                      style: ElevatedButton.styleFrom(
                        backgroundColor: const Color(0xFF66ccff),
                        foregroundColor: Colors.black,
                      ),
                      child: const Text('重试'),
                    ),
                  ],
                ),
              ),
            );
          }
          return const HomePage();
        },
      ),
    );
  }

  ThemeData _buildTheme(Brightness brightness) {
    final isDark = brightness == Brightness.dark;
    final Color bgColor = isDark ? const Color(0xFF1a1a2e) : const Color(0xFFF5F5F5);
    final Color surfaceColor = isDark ? const Color(0xFF16213e) : Colors.white;
    final Color textColor = isDark ? Colors.white : const Color(0xFF2D2D2D);
    final Color accent = const Color(0xFF66ccff);

    final baseTextTheme = TextTheme(
      headlineLarge: TextStyle(color: textColor, fontWeight: FontWeight.bold),
      headlineMedium: TextStyle(color: textColor, fontWeight: FontWeight.w600),
      bodyLarge: TextStyle(color: textColor),
      bodyMedium: TextStyle(color: textColor.withOpacity(0.8)),
      bodySmall: TextStyle(color: textColor.withOpacity(0.6)),
    );
    final textTheme = GoogleFonts.notoSansScTextTheme(baseTextTheme);

    return ThemeData(
      useMaterial3: true,
      brightness: brightness,
      colorSchemeSeed: accent,
      scaffoldBackgroundColor: bgColor,
      cardColor: surfaceColor,
      appBarTheme: AppBarTheme(
        backgroundColor: surfaceColor,
        foregroundColor: textColor,
        elevation: 0,
        centerTitle: true,
      ),
      navigationBarTheme: NavigationBarThemeData(
        backgroundColor: surfaceColor,
        indicatorColor: accent.withOpacity(0.2),
        labelTextStyle: WidgetStateProperty.resolveWith((states) {
          if (states.contains(WidgetState.selected)) {
            return TextStyle(color: accent, fontSize: 12, fontWeight: FontWeight.w600);
          }
          return TextStyle(color: textColor.withOpacity(0.6), fontSize: 12);
        }),
        iconTheme: WidgetStateProperty.resolveWith((states) {
          if (states.contains(WidgetState.selected)) {
            return IconThemeData(color: accent, size: 24);
          }
          return IconThemeData(color: textColor.withOpacity(0.6), size: 24);
        }),
      ),
      textTheme: textTheme,
      primaryTextTheme: textTheme,
      dividerTheme: DividerThemeData(
        color: textColor.withOpacity(0.08),
        thickness: 0.5,
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: isDark ? const Color(0xFF0f3460) : const Color(0xFFEEEEEE),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: BorderSide.none,
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: const BorderSide(color: Color(0xFF66ccff), width: 2),
        ),
        labelStyle: TextStyle(color: textColor.withOpacity(0.6)),
        hintStyle: TextStyle(color: textColor.withOpacity(0.3)),
      ),
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          backgroundColor: accent,
          foregroundColor: Colors.black,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
          padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 14),
          textStyle: const TextStyle(fontSize: 15, fontWeight: FontWeight.w600),
        ),
      ),
      switchTheme: SwitchThemeData(
        thumbColor: WidgetStateProperty.resolveWith((states) {
          if (states.contains(WidgetState.selected)) return accent;
          return Colors.grey;
        }),
        trackColor: WidgetStateProperty.resolveWith((states) {
          if (states.contains(WidgetState.selected)) return accent.withOpacity(0.3);
          return Colors.grey.withOpacity(0.2);
        }),
      ),
    );
  }
}

class _SplashScreen extends StatelessWidget {
  const _SplashScreen();

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF1a1a2e),
      body: Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              width: 80,
              height: 80,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                border: Border.all(color: const Color(0xFF66ccff), width: 3),
              ),
              child: const Icon(
                Icons.lan_outlined,
                color: Color(0xFF66ccff),
                size: 40,
              ),
            ),
            const SizedBox(height: 24),
            const Text(
              'QtEasyTier',
              style: TextStyle(
                color: Colors.white,
                fontSize: 28,
                fontWeight: FontWeight.bold,
                letterSpacing: 1.2,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              'Mobile',
              style: TextStyle(
                color: const Color(0xFF66ccff).withOpacity(0.7),
                fontSize: 14,
                letterSpacing: 4,
              ),
            ),
            const SizedBox(height: 32),
            SizedBox(
              width: 24,
              height: 24,
              child: CircularProgressIndicator(
                strokeWidth: 2.5,
                valueColor: AlwaysStoppedAnimation<Color>(
                  const Color(0xFF66ccff).withOpacity(0.7),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
