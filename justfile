update:
    npins update
    $(nix-build --no-out-link -A mitmCache.updateScript)

build:
    nix-build --no-out-link