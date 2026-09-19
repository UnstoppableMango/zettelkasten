# The Android dev shell, separate from the default one because the SDK, NDK,
# and a system image are several gigabytes of unfree closure and none of it is
# needed to edit Go code.
{
  androidenv,
  git,
  gomobile,
  gradle,
  inputsFrom ? [ ],
  jdk17,
  mkShellNoCC,
}:
let
  # gomobile compiles against the SDK platform matching its -androidapi and
  # refuses to run when that platform is absent, so the oldest Android the app
  # supports is pinned here rather than left to whichever platform the SDK
  # happens to ship. The NDK accepts 21 through 35; 24 is the floor that still
  # reaches ordinary phones.
  #
  # Both levels are the app's too: android/app/build.gradle.kts sets minSdk to
  # the first and compileSdk to the second.
  androidApi = "24";
  androidCompileApi = "35";

  androidBuild = androidenv.composeAndroidPackages {
    includeNDK = true;
    platformVersions = [
      androidApi
      androidCompileApi
    ];
    buildToolsVersions = [ "35.0.0" ];
  };

  # composeAndroidPackages fetches a system image for every platform in
  # platformVersions, so the emulator is composed on its own. Sharing
  # androidBuild's list would mean a second image, for an Android nothing is
  # tested on, at a gigabyte and a half.
  androidEmulator = androidenv.composeAndroidPackages {
    includeEmulator = true;
    includeSystemImages = true;
    systemImageTypes = [ "google_apis" ];
    abiVersions = [ "x86_64" ];
    platformVersions = [ androidCompileApi ];
  };
in
mkShellNoCC {
  inherit inputsFrom;

  packages = [
    # gomobile builds mobile/ into an .aar. Its wrapper puts the SDK on PATH
    # and sets ANDROID_HOME; the JDK is what assembles the archive once the NDK
    # has compiled the Go side, and what Gradle runs on to build the app around
    # it.
    (gomobile.override { androidPkgs = androidBuild; })
    gradle
    jdk17

    # git daemon serves the notebook the instrumented tests publish to. go-git
    # speaks that protocol in process, where a remote on the local filesystem
    # would need git-upload-pack on the device.
    git
  ];

  # Gradle finds the SDK through ANDROID_HOME. gomobile's wrapper sets the same
  # variable for itself, but only inside its own process, so Gradle needs it
  # here.
  ANDROID_HOME = "${androidBuild.androidsdk}/libexec/android-sdk";

  # The emulator and its system image live in their own SDK root, and
  # android/emulator-test.sh reads this to find them.
  SLIP_EMULATOR_SDK = "${androidEmulator.androidsdk}/libexec/android-sdk";
  SLIP_ANDROID_API = androidCompileApi;

  # gomobile's wrapper appends its own store path to GOPATH, so an unset GOPATH
  # leaves the read-only store as the only entry and the module cache has
  # nowhere to go. Setting Go's own default here puts a writable directory in
  # front of it.
  shellHook = ''
    export GOPATH="''${GOPATH:-$HOME/go}"
    cd "$(git rev-parse --show-toplevel)/android"
  '';
}
