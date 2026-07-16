# Security Policy

## Supported versions

Security fixes are applied to the current release line. Users should update to the latest Google Play or GitHub release.

## Reporting a vulnerability

Do not include real IMEI values, seller/customer data, addresses, phone numbers, purchase tokens, keystores, service-account JSON, or complete inspection reports in a public issue. Use GitHub's private vulnerability reporting for this repository when available. Include a minimal reproduction, affected version, Android version, and expected impact with sanitized data.

## Security design

- Inspection evidence and temporary audio are app-private.
- FileProvider is non-exported and grants temporary URI access only during explicit sharing.
- Cleartext network traffic is disabled. The inspection engine has no backend.
- Imported backups are strict JSON with a supported format version, bounded size/count, enum validation, and generated local IDs.
- Release shrinking is enabled; sensitive values and purchase tokens are not logged.
- Keystores and Play credentials are supplied only through GitHub Secrets and temporary runner files.
- Non-public Android components are not exported.

The app intentionally avoids hidden APIs, root, shell execution, accessibility services, overlays, broad storage permissions, and automatic restricted identifier access.

