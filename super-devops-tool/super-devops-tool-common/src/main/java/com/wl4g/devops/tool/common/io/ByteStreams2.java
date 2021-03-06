/*
 * Copyright 2017 ~ 2025 the original author or authors. <wanglsir@gmail.com, 983708408@qq.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wl4g.devops.tool.common.io;

import java.io.IOException;
import java.io.InputStream;

import com.google.common.io.ByteStreams;

/**
 * Byte stream utility. </br>
 * {@link ByteStreams}
 * 
 * @author Wangl.sir <wanglsir@gmail.com, 983708408@qq.com>
 * @version v1.0 2019年10月23日
 * @since
 */
public abstract class ByteStreams2 {

	/**
	 * Read all data from the input stream and turn it into a string. Note: the
	 * stream will not be closed after reading. Warning: the data size should be
	 * estimated before use. If the data volume is too large, there may be a
	 * risk of memory leakage, Also note that InputStream blocking should be
	 * set, such as waitForCompleted().
	 * 
	 * @param in
	 * @return
	 */
	public static String readFullyToString(InputStream in) {
		return readFullyToString(in, "UTF-8");
	}

	/**
	 * Read all data from the input stream and turn it into a string. Note: the
	 * stream will not be closed after reading. Warning: the data size should be
	 * estimated before use. If the data volume is too large, there may be a
	 * risk of memory leakage, Also note that InputStream blocking should be
	 * set, such as waitForCompleted().
	 * 
	 * @param in
	 * @param charset
	 * @return
	 * @throws IllegalStateException
	 */
	public static String readFullyToString(InputStream in, String charset) throws IllegalStateException {
		try {
			StringBuffer msg = new StringBuffer();
			byte[] buf = new byte[4096];
			int n = 0;
			while ((n = in.read(buf)) > 0) {
				msg.append(new String(buf, 0, n));
			}
			return msg.toString();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

}