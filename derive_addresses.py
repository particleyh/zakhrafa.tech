#!/usr/bin/env python3
import sys
from bitcoinlib.keys import HDKey

def derive_all_addresses(xpub, count=3):
    results = []
    for i in range(count):
        path = f'0/{i}'
        legacy_key = HDKey(xpub, witness_type='legacy')
        p2sh_key = HDKey(xpub, witness_type='p2sh-segwit')
        segwit_key = HDKey(xpub, witness_type='segwit')
        p2tr_key = HDKey(xpub, witness_type='segwit')

        child_legacy = legacy_key.subkey_for_path(path)
        child_p2sh = p2sh_key.subkey_for_path(path)
        child_segwit = segwit_key.subkey_for_path(path)
        child_tr = p2tr_key.key_for_path(path)

        results.append({
            'index': i,
            'p2pkh': child_legacy.address(),
            'p2sh': child_p2sh.address(),
            'p2wpkh': child_segwit.address(),
            'p2tr': child_tr.address(script_type='p2tr'),
        })
    return results

def main():
    if len(sys.argv) < 2:
        print("Usage: python3 derive_addresses.py <xpub|ypub|zpub> [count]")
        sys.exit(1)
    xpub = sys.argv[1]
    count = int(sys.argv[2]) if len(sys.argv) > 2 else 3
    results = derive_all_addresses(xpub, count)
    for r in results:
        print(f"--- Address set {r['index']} ---")
        print(f"  P2PKH (1...)    : {r['p2pkh']}")
        print(f"  P2SH  (3...)    : {r['p2sh']}")
        print(f"  P2WPKH (bc1q..) : {r['p2wpkh']}")
        print(f"  P2TR   (bc1p..) : {r['p2tr']}")
        print()

if __name__ == '__main__':
    main()
