# Logibooks Scanner - CI Caching Fix

This repository contains the fix for GitHub Actions caching failures in the CI workflow.

## Quick Summary

**Problem**: CI builds were experiencing cache failures with HTTP 400 errors  
**Cause**: Cache keys were too long (exceeded 512 character limit)  
**Solution**: Added `cache-encryption-key` and shortened job name

## What Was Fixed

### The Issue

The CI workflow logs showed errors like:
```
Failed to restore gradle-home-v1|Linux|build-and-test[...]: Error: Cache service responded with 400
```

This happened because:
1. The automatically generated cache key was too long
2. GitHub's cache service has a ~512 character limit for cache keys
3. The combination of `gradle-home-v1|Linux|build-and-test[hash]-commit-sha` exceeded this limit

### The Solution

**Changes Made**:
1. ✅ Shortened job name: `build-and-test` → `build`
2. ✅ Added `cache-encryption-key` to setup-gradle action
3. ✅ Enabled Gradle configuration cache in `gradle.properties`

**Files Changed**:
- `.github/workflows/ci.yml`: Updated with cache-encryption-key
- `gradle.properties`: Added `org.gradle.configuration-cache=true`
- `CACHING_FIX.md`: Detailed technical analysis
- `GITHUB_SECRETS_SETUP.md`: Setup instructions for the encryption key

## Setup Required

⚠️ **Action Required**: You must add the `GRADLE_ENCRYPTION_KEY` secret to your GitHub repository.

### Quick Setup (2 minutes)

1. Generate a key:
   ```bash
   openssl rand -base64 16
   ```

2. Add to GitHub:
   - Go to **Settings** → **Secrets and variables** → **Actions**
   - Click **New repository secret**
   - Name: `GRADLE_ENCRYPTION_KEY`
   - Value: Paste the generated key
   - Click **Add secret**

For detailed instructions, see [GITHUB_SECRETS_SETUP.md](GITHUB_SECRETS_SETUP.md)

## Benefits

After this fix:
- ✅ Gradle dependencies are cached properly
- ✅ CI builds run faster (dependencies don't need to be re-downloaded)
- ✅ No more HTTP 400 cache errors
- ✅ Configuration cache speeds up builds even more
- ✅ More secure cache with encryption

## Testing

To verify the fix works:
1. Set up the `GRADLE_ENCRYPTION_KEY` secret (see above)
2. Push a commit or manually trigger the CI workflow
3. Check the workflow logs for:
   ```
   Gradle User Home cache restored from key: gradle-home-v1|...
   ```
4. At the end, you should see:
   ```
   Post Setup Gradle
   Gradle User Home cache saved with key: gradle-home-v1|...
   ```

## Documentation

- **[CACHING_FIX.md](CACHING_FIX.md)**: Comprehensive technical analysis of the issue
- **[GITHUB_SECRETS_SETUP.md](GITHUB_SECRETS_SETUP.md)**: Detailed setup guide for the encryption key

## References

- [Gradle Setup Action Documentation](https://github.com/gradle/actions/blob/main/setup-gradle/README.md)
- [GitHub Actions Cache Documentation](https://docs.github.com/en/actions/using-workflows/caching-dependencies-to-speed-up-workflows)
- [Gradle Configuration Cache](https://docs.gradle.org/current/userguide/configuration_cache.html)
