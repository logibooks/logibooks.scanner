# Setting Up GitHub Actions Secrets

This document explains how to set up the required GitHub Actions secrets for this repository.

## GRADLE_ENCRYPTION_KEY

The `GRADLE_ENCRYPTION_KEY` secret is required for the CI workflow to cache Gradle dependencies efficiently.

### Why is this needed?

- **Fixes caching issues**: Without this key, cache keys become too long and cause HTTP 400 errors
- **Enables configuration cache**: Allows Gradle to use its configuration cache feature for faster builds
- **Security**: Encrypts the Gradle configuration cache to prevent potential security issues

### Setup Instructions

1. **Generate an encryption key**:
   ```bash
   openssl rand -base64 16
   ```
   
   This will output something like: `abc123XYZ789def456gh==`

2. **Add the secret to GitHub**:
   - Go to your repository on GitHub
   - Click on **Settings** (repository settings)
   - In the left sidebar, click **Secrets and variables** → **Actions**
   - Click **New repository secret**
   - Name: `GRADLE_ENCRYPTION_KEY`
   - Value: Paste the key generated in step 1
   - Click **Add secret**

3. **Verify**:
   - The CI workflow will automatically use this secret
   - No changes to the workflow file are needed
   - The next CI run should show successful caching

### What if the secret is not set?

If the `GRADLE_ENCRYPTION_KEY` secret is not configured:
- The workflow will fail with an error about the missing secret
- OR caching will be disabled and builds will be slower
- You'll see warnings in the workflow logs

### Security Notes

- Never commit this key to the repository
- Treat it like a password
- If compromised, generate a new one and update the secret
- The key only affects CI caching; it doesn't expose your code or data
