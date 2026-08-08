
from cryptography.fernet import Fernet
import hashlib
import hmac
import os

import base64

from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.kdf.hkdf import HKDF

# deriving sub keys

def derive_subkey(master_key: bytes):
    hkdf_enc=HKDF(
        algorithm=hashes.SHA256(),
        length=32,
        salt=None,
        info=b"fernet-encryption-key"
    )

    raw_enc_key=hkdf_enc.derive(master_key)
    key_enc=base64.urlsafe_b64encode(raw_enc_key)

    hkdf_hash=HKDF(
        algorithm=hashes.SHA256(),
        length=32,
        salt=None,
        info=b"hmac-authentication-key"
    )
    key_hash= hkdf_hash.derive(master_key)

    return key_enc , key_hash

def run_decryption(enc_file_path:str, key_file_path:str , output_dir: str):
    with open(key_file_path,"r") as kfile:
        key_data=kfile.read().strip()
        master_key_hex , cert = key_data.split("|")
        master_key_hex=master_key_hex.strip()
        cert=cert.strip()
        master_key=bytes.fromhex(master_key_hex)

    key_enc , key_hash = derive_subkey(master_key)

    h=hmac.new(key_hash,None,hashlib.sha3_256)

    with open(enc_file_path,"rb") as file:
        while True:
            size_data=file.read(4)
            if not size_data:
                break

            size=int.from_bytes(size_data,"big")
            enc_data=file.read(size)
            h.update(enc_data)

    cert_auth=h.hexdigest()



    if not hmac.compare_digest(cert_auth,cert):
        raise ValueError("Authentication Failed! File or Key has been tampered with.")

    #decryption part

    fernet_obj = Fernet(key_enc)
    base_name=os.path.basename(enc_file_path)

    if base_name.endswith(".enc"):
        out_name= base_name[:-4]

    else:
        out_name=f"decrypted_{base_name}"

    dec_file_path=os.path.join(output_dir , out_name)


    with open(enc_file_path,"rb") as infile , open (dec_file_path,"wb") as outfile:
        while True:
            size_data=infile.read(4)
            if not size_data:
                break

            size=int.from_bytes(size_data,"big")
            enc_data=infile.read(size)
            dec_data= fernet_obj.decrypt(enc_data)
            outfile.write (dec_data)

    return dec_file_path


