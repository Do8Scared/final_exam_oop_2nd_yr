# Garahe ni Mateicla POS

## What changed
- Database credentials are no longer hardcoded.
- Admin access is now driven by configuration instead of a fixed PIN.
- Checkout now re-checks and locks stock inside the transaction.
- Cart items snapshot their unit price so totals stay stable.

## Required configuration
Set these before running:

- `POS_DB_URL`
- `POS_DB_USER`
- `POS_DB_PASSWORD`
- `POS_ADMIN_PIN`

You can also pass JVM properties instead:

- `-Dpos.db.url=...`
- `-Dpos.db.user=...`
- `-Dpos.db.password=...`
- `-Dpos.admin.pin=...`

## Compile
```powershell
Set-Location "G:\Github Repos\Main\Main"
$files = Get-ChildItem -Recurse -Filter *.java | ForEach-Object { $_.FullName }
javac -d out $files
```

## Run
```powershell
Set-Location "G:\Github Repos\Main\Main"
java -cp out Main.Main
```

If the database env vars are missing, the app will fail fast with a clear message instead of using secrets from source code.

