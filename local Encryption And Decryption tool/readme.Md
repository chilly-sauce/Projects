# Secure Chunk-Based File Encryption System

A local desktop application for file encryption and integrity verification.
Built around an Encrypt-then-MAC architecture with chunk-based streaming
for memory-efficient processing of large files.

---

## Security Decisions

**Encrypt-then-MAC**
Verification happens before decryption. A tampered file fails the MAC
check immediately and execution aborts — the decryption engine never
processes untrusted ciphertext, eliminating padding oracle attack surface.

**Subkey Derivation via HKDF**
The master key is never used directly for encryption or authentication.
HKDF derives cryptographically independent subkeys for each operation —
a weakness in one operation cannot create mathematical relationships that
compromise the other.

**Chunk-Level Authentication**
Each chunk is authenticated independently. An attacker cannot reorder,
replay, or drop individual chunks without failing authentication — even
if the overall file bytes remain unchanged.

**Constant-Time MAC Comparison**
Standard equality checks short-circuit on the first mismatched byte,
allowing attackers to reconstruct valid MACs through timing measurement.
Constant-time comparison runs to completion regardless of mismatch
position, eliminating this side channel.

**Local Processing Only**
Encryption happens entirely on the user's machine. Uploading plaintext
to a server for encryption defeats the purpose — the server becomes a
trust requirement. Local processing means no third party ever sees
unencrypted data.

---

## Architecture

### Encryption Pipeline

1. User selects a file for encryption
2. System generates a cryptographically secure master key via CSPRNG
3. HKDF derives two independent subkeys from the master key:
   - Encryption subkey
   - Authentication subkey
4. File is read in 64 KB chunks:
   - Each chunk is encrypted using the encryption subkey
   - Each encrypted chunk is fed into an HMAC instance using
     the authentication subkey
5. Output:
   - `<filename>.enc` — encrypted file
   - `<filename>.key` — credentials file containing master key,
     HKDF salt, authentication certificate, and cryptographic parameters

### Decryption Pipeline

1. Receiver provides the encrypted file and credentials file
2. Master key and parameters are parsed from credentials file
3. HKDF reconstructs both subkeys
4. **Pre-decryption verification:**
   - Encrypted file is read in chunks
   - HMAC is recomputed using the authentication subkey
   - Computed digest is compared against the certificate
     using constant-time comparison
5. If verification fails — execution terminates immediately
6. If verification succeeds — file is decrypted chunk by chunk
   and written to disk

---

## Key Distribution

The credentials file must reach the receiver through a separate channel
from the encrypted file. Sending both over the same channel defeats
the security model. Recommended approaches:

- Physical media (USB drive)
- End-to-end encrypted messaging (Signal, WhatsApp with E2E enabled)

This separates the two attack surfaces — an attacker intercepting the
encrypted file gains nothing without the credentials file.

---

## Tech Stack

- Python 3
- `cryptography` — Fernet, HKDF, HMAC
- `tkinter` — GUI
- PyInstaller — standalone packaging (no Python installation required)

---

## Limitations

- Single-use keys — no key rotation (appropriate for transfer use case,
  not long-term storage)
- Key distribution is manual — no automated secure channel for
  credentials delivery
- No forward secrecy — key compromise exposes all files encrypted
  under that key

---

## Honest Attribution

Cryptographic architecture, security decisions, chunk streaming logic,
and HMAC implementation by me. GUI scaffolding was AI-assisted.