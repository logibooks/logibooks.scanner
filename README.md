# MT93ScannerClient (Newland MT93)

Android app implementing:
1) Login to REST server -> receives JWT bearer token
2) Wait for barcode scans via MT93 broadcast API (Option 3)
3) Send scanned code to server with `Authorization: Bearer <token>` and receive `{ "match": true/false }`
4) Display last scanned code + response, repeat; user can logout.

## Scanner integration (MT93 / Newland)
The app listens for broadcast:
- Action: `nlscan.action.SCANNER_RESULT`
- Extras:
  - `SCAN_BARCODE1` (String)
  - `SCAN_BARCODE2` (String)
  - `SCAN_BARCODE_TYPE` (int)
  - `SCAN_STATE` = `ok` or `fail`

No need to start/stop scans when using the hardware scan key. The app only receives results.

## Server API contracts

### Login
`POST /login` (AllowAnonymous)
Request JSON:
```json
{ "Email": "user@x.com", "Password": "secret" }
```
Response JSON (200):
```json
{
  "Id": 1,
  "FirstName": "John",
  "LastName": "Doe",
  "Patronymic": "P",
  "Email": "user@x.com",
  "Roles": ["User"],
  "Token": "eyJhbGciOi..."
}
```

### Scan check
`POST /scan/check` (Authorize)
Request JSON:
```json
{ "code": "1234567890" }
```
Response JSON:
```json
{ "match": true }
```

## Build / Run
- Open the folder in Android Studio
- Edit the Server Base URL on login screen (must include scheme, e.g. `https://example.com/`)
- Run on MT93 device (Android)

## Notes
- Retrofit requires `baseUrl` ending with `/`. The app normalizes it automatically.
