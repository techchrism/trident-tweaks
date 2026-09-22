{
  sources ? import ./npins,
  pkgs ? import sources.nixpkgs {}
}:
pkgs.stdenv.mkDerivation (finalAttrs: {
  pname = "trident-tweaks";
  version = "2.1.0";
  src = pkgs.nix-gitignore.gitignoreSource [] ./.;

  nativeBuildInputs = with pkgs; [ gradle_9 openjdk25_headless ];

  mitmCache = pkgs.gradle_9.fetchDeps {
    pkg = finalAttrs.finalPackage;
    data = ./deps.json;
  };

  installPhase = ''
    cp build/libs/trident-tweaks.jar $out
  '';
})