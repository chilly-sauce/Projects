# Java TOTP Authenticator Portal

A secure, fully functional Two-Factor Authentication (2FA) application built in Java. This project implements the Time-Based One-Time Password (TOTP) algorithm from scratch to generate and validate 6-digit verification codes compatible with Google and Microsoft Authenticator.

## The Need
To provide an enterprise-ready verification layer for user authentication, ensuring that even if a password is compromised, access is protected by a time-sensitive cryptographic token tied to a physical device.

---

## How It Works (Under the Hood)

### 1. Secret Generation & Client Sync
* **The Secret:** A secure foundation is established by generating a 20-byte key using a Cryptographically Secure Pseudorandom Number Generator (CSPRNG).
* **The QR Code:** To share this secret with the user's authenticator app, the byte array is Base32 encoded and formatted into a standard `otpauth://` URI (containing the type, label, secret, and issuer). This URI is then rendered into a scannable QR code.

### 2. Time Windows & Synchronization
* **30-Second Windows:** Instead of generating a new OTP every single second, the current Unix epoch time is divided by 30. This creates a 30-second time step/window where the OTP remains constant.
* **Drift Tolerance:** To account for network latency or slight clock synchronization errors between the client and server, the validation system checks the user's input against three windows: the current window ($t$), the previous window ($t-1$), and the next window ($t+1$).

### 3. Generating the 6-Digit OTP (The Algorithm)
1. **Hashing:** The 20-byte secret and the current 30-second time window are hashed together using HMAC-SHA1.
2. **Dynamic Truncation:** To extract the OTP from the resulting hash:
   * We look at the very last 4 bits of the hash to use as an index offset.
   * Starting from that index, we extract exactly 31 bits. (We strictly use 31 bits instead of 32 by masking with `0x7F` to avoid negative number/signed integer confusion in Java).
3. **Modulo Reduction:** This extracted 31-bit integer is a massive number. To get the final, user-friendly 6-digit code, we simply calculate the number modulo `1,000,000`.

---

## Built-In Security Defenses
* **Replay Attack Prevention:** Tracks the last successfully authenticated time step to ensure a valid 30-second window cannot be reused by an intercepted code.
* **Timing Attack Prevention:** Utilizes `MessageDigest.isEqual()` on UTF-8 byte arrays for constant-time validation, preventing side-channel character guessing.
* **Brute-Force Protection:** Implements a strict 3-strike lockout policy, disabling the input portal after three failed attempts.

## Tech Stack
* **Language:** Java
* **UI:** JavaFX
* **Libraries:** ZXing (QR Generation), Apache Commons Codec (Base32)
