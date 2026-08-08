
from cryptography.fernet import Fernet
import hashlib
import hmac
import base64
import os

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

def run_encryption (input_file_path : str , output_dir : str):
    base_name= os.path.basename(input_file_path)
    # generating master key
    master_key=os.urandom(32)

    key_enc , key_hash=derive_subkey(master_key)

    fernet_obj = Fernet(key_enc)
    h=hmac.new(key_hash,None,hashlib.sha3_256)
    size =64 *1024

    enc_file_path = os.path.join(output_dir , f"{base_name}.enc")

    with open(input_file_path,"rb") as infile , open (enc_file_path,"wb") as outfile:
        while True:
            chunk= infile.read(size)
            if(len(chunk)==0):
                break

            encchunk=fernet_obj.encrypt(chunk)
            h.update(encchunk)
            outfile.write(len(encchunk).to_bytes(4,"big"))
            outfile.write(encchunk)

    cert =h.hexdigest()

    key_file_path=os.path.join(output_dir,f"{base_name}.key")
    with open(key_file_path,"w") as kfile:
        kfile.write(f"{master_key.hex()}|{cert}")

    return enc_file_path , key_file_path



