// Copyright 2024 The NEXTSTACK Authors.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.lnjoying.justice.operation.utils;

import cn.hutool.core.codec.Base58;
import com.lnjoying.justice.operation.config.NodeConfig;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Utils;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

public class CipherUtil
{
	static byte [] private_key_byte;
	static byte[] private_prefix = {'c', 'r', 'y', 'p', 't', '.'};
	static byte[] pub_prefix = {'i', 'd', '.'};
	public static byte[] unitByteArray(byte[] byte1, byte[] byte2)
	{
		byte[] unitByte = new byte[byte1.length + byte2.length];
		System.arraycopy(byte1, 0, unitByte, 0, byte1.length);
		System.arraycopy(byte2, 0, unitByte, byte1.length, byte2.length);
		return unitByte;
	}

	public static String encodeChecked(byte[] payload)
	{
		byte[] addressBytes = new byte[payload.length + 4];
		System.arraycopy(payload, 0, addressBytes, 0, payload.length);
		byte[] checksum = Sha256Hash.hashTwice(payload, 0, payload.length);
		System.arraycopy(checksum, 0, addressBytes, payload.length, 4);
		return Base58.encode(addressBytes);
	}

	public static boolean checkSign(Map<String, String> exten_info, String nonce)
	{
		if (exten_info.get("sign") == null ||exten_info.get("sign_algo") == null
				|| exten_info.get("sign_at") == null || exten_info.get("origin_id") == null
				|| exten_info.get("sign").isEmpty() ||exten_info.get("sign_algo").isEmpty()
				|| exten_info.get("sign_at").isEmpty() || exten_info.get("origin_id").isEmpty())
		{
			return false;
		}

		String sign_msg =  nonce + exten_info.get("origin_id") +  exten_info.get("sign_at");
		return checkSign(sign_msg, exten_info.get("sign"), exten_info.get("origin_id"));
	}

	public static boolean checkSign(String message, String sign, String src_node_id)
	{
		try
		{
			byte[] msg_byte = message.getBytes();
			byte hash_message[] = Sha256Hash.hashTwice(msg_byte, 0, msg_byte.length);
			Sha256Hash _hash = Sha256Hash.wrap(hash_message);

			String r_sign_s = sign.substring(2, 66);
			String l_sign_s = sign.substring(66, 130);

			byte[] _sign = Utils.HEX.decode(sign.substring(0, 2));
			int recId = (_sign[0] - 27) & 3;
			BigInteger r_sign = new BigInteger(r_sign_s, 16);
			BigInteger l_sign = new BigInteger(l_sign_s, 16);
			ECKey.ECDSASignature _sig = new ECKey.ECDSASignature(r_sign, l_sign);
			ECKey recoveredKey = ECKey.recoverFromSignature(recId, _sig, _hash, true);

			byte[] pub = recoveredKey.getPubKey();
			byte[] key_id = Utils.sha256hash160(pub);
			byte[] node_info = unitByteArray(pub_prefix, key_id);
			String derived = encodeChecked(node_info);

			return derived.equals(src_node_id);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
	}

	public static NodeConfig gen_node()
	{
		ECKey ecKey = new ECKey();
		byte[] privateKeyBytes = ecKey.getPrivKeyBytes();
		;
		byte[] pub = ecKey.getPubKey();
		byte[] key_id = Utils.sha256hash160(pub);
		byte[] node_info = unitByteArray(pub_prefix, key_id);
		String node_id = encodeChecked(node_info);

		NodeConfig nodeConfig = new NodeConfig();
		nodeConfig.setRegion_id("admin");
		nodeConfig.setNode_id(node_id);
		nodeConfig.setCore_version("0.0.1");
		nodeConfig.setProtocol_version("0.0.1");
		nodeConfig.setNode_name("admin");
		nodeConfig.setPrivate_key(private_key_str(privateKeyBytes));
		private_key_byte = privateKeyBytes;
		return nodeConfig;
	}

	private static String private_key_str(byte [] srcKey)
	{
		byte[] key_byte = unitByteArray(private_prefix, srcKey);
		return encodeChecked(key_byte);
	}

	public static String sign(String message)
	{
		ECKey ecKey = ECKey.fromPrivate(private_key_byte);
		byte[] msg_byte = message.getBytes();
		byte hash_message[] = Sha256Hash.hashTwice(msg_byte, 0, msg_byte.length);
		Sha256Hash _hash = Sha256Hash.wrap(hash_message);

		ECKey.ECDSASignature sig = ecKey.sign(_hash);
		byte recId = ecKey.findRecoveryId(_hash, sig);
		int headerByte = recId + 27 + (ecKey.isCompressed() ? 4 : 0);
		byte[] sigData = new byte[65];
		sigData[0] = (byte)headerByte;
		System.arraycopy(Utils.bigIntegerToBytes(sig.r, 32), 0, sigData, 1, 32);
		System.arraycopy(Utils.bigIntegerToBytes(sig.s, 32), 0, sigData, 33, 32);
		return Utils.HEX.encode(sigData);
	}

	public static Map<String, String> sign_exten(String message, String node_id)
	{
		Map<String, String> exten_map = new HashMap<>();
		String sigtAt = String.valueOf(System.currentTimeMillis());
		String sig = sign(message+sigtAt);
		exten_map.put("origin_id", node_id);
		exten_map.put("sign_at", sigtAt);
		exten_map.put("sign",sig);
		exten_map.put("sign_algo","ecdsa");
		return exten_map;
	}

	public static void import_private_key(String privatekey_str)
	{
		byte [] pri_byte = Base58.decodeChecked(privatekey_str);
		private_key_byte = new byte[pri_byte.length - private_prefix.length];
		System.arraycopy(pri_byte, private_prefix.length, private_key_byte, 0, pri_byte.length - private_prefix.length);
	}


//	public static void main(String[] args) throws Exception
//	{
//		String signMsg = String.format("%s%s%s", "a9aa3917945c430596be04f6a0f197ec", "FFAHjytrnh2HytJyL6vC44bkQfWPvgQzMCGdg", "1622534766040");
//		checkSign(signMsg, "208656ff02db20370ff7589cb947fabd795c04388089a02c974832ac4396c49f1f0993e7155065b3d7281dca2862baf73fe283533904c759f0f868a00d24fa0715", "FFAHjytrnh2HytJyL6vC44bkQfWPvgQzMCGdg");
//	}

//	public void keyRecoveryTestVector() {
//		String message = "dbc key verification\n";
//		byte[] msg = message.getBytes();
//		Sha256Hash.hashTwice(msg, 0, msg.length);
//		byte hash1[] = Sha256Hash.hashTwice(msg, 0, msg.length);
//		Sha256Hash hash2 = Sha256Hash.of(message.getBytes());
//		Sha256Hash dbc_hash = Sha256Hash.wrap("14132b0165f8e50ba711baa8affbeea5f9f6ff8c5e5a01734dd07b9227154872");
//		byte[] dbc_sign = org.bitcoinj.core.Utils.HEX.decode("20c8df41bf0031660e16c9f20a1e20ab52d9b13b462ef060b439fb61342d33f76839efc07c0abdcb2a985f2a9401bebda028756fc030499dec811b846726533bd7");
//		BigInteger r_sign = new BigInteger("c8df41bf0031660e16c9f20a1e20ab52d9b13b462ef060b439fb61342d33f768", 16);
//		BigInteger l_sign = new BigInteger("39efc07c0abdcb2a985f2a9401bebda028756fc030499dec811b846726533bd7", 16);
//		byte[] r = org.bitcoinj.core.Utils.bigIntegerToBytes(r_sign, 32);
//		byte[] l = org.bitcoinj.core.Utils.bigIntegerToBytes(l_sign, 32);
////        System.arraycopy(dbc_sign,1, r_sign, 0, 32);
////        System.arraycopy(dbc_sign,33, l_sign, 0, 32);
//		//ECKey.ECDSASignature dbc_sig = ECKey.ECDSASignature.decodeFromDER(dbc_sign);
//		ECKey.ECDSASignature dbc_sig = new ECKey.ECDSASignature(r_sign, l_sign);
//		byte[] src_nodeinfo = Base58.decodeChecked("2gfpp3MAB4Ckr2NghGJ12KumnEtEFV4YV8V1rCBupRS");
//		byte[] sss = Base58.decode("2gfpp3MAB4Ckr2NghGJ12KumnEtEFV4YV8V1rCBupRS");
//
//		int recId = (dbc_sign[0] - 27) & 3;
//		ECKey recoveredKey = ECKey.recoverFromSignature(recId, dbc_sig, dbc_hash, true);
//		byte[] pub = recoveredKey.getPubKey();
//		byte[] key_id = org.bitcoinj.core.Utils.sha256hash160(pub);
//		byte[] prefix = {'n', 'o', 'd', 'e', '.', '0', '.'};
//		byte[] node_info = unitByteArray(prefix, key_id);
//		String nos = encodeChecked(node_info);
//		System.out.print(nos);
//	}
}
