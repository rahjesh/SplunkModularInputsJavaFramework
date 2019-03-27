package com.splunk.modinput.kinesis;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.splunk.modinput.kinesis.KinesisModularInput.MessageReceiver;

public class JSONOnlyMessageHandler extends AbstractMessageHandler {

	
	private final CharsetDecoder decoder = Charset.forName("UTF-8")
			.newDecoder();
	@Override
	public void handleMessage(ByteBuffer rawBytes,String seqNumber,
			String partitionKey, MessageReceiver context) throws Exception {

		String record = decoder.decode(rawBytes).toString();
		String text = stripNewlines(record);

		transportMessage(text, String.valueOf(System.currentTimeMillis()), "");

	}

	public static List<String> chunkData(String text, int size) {

		List<String> ret = new ArrayList<String>((text.length() + size - 1)
				/ size);

		for (int start = 0; start < text.length(); start += size) {
			ret.add(text.substring(start, Math.min(text.length(), start + size)));
		}
		return ret;
	}

	@Override
	public void setParams(Map<String, String> params) {
		// Do nothing , params not used

	}

}
