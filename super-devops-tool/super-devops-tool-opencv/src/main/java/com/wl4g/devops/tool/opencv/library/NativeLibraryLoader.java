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
package com.wl4g.devops.tool.opencv.library;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.equalsAnyIgnoreCase;
import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS;
import static org.apache.commons.lang3.SystemUtils.OS_ARCH;

import java.awt.IllegalComponentStateException;
import java.io.File;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * OpenCv dynamic link library loader.</br>
 * 
 * @author Wangl.sir &lt;Wanglsir@gmail.com, 983708408@qq.com&gt;
 * @version v1.0.0 2019-12-01
 * @see <a href=
 *      "https://docs.opencv.org/2.4/doc/tutorials/introduction/desktop_java/java_dev_intro.html">opencv-java-install</a>
 * @since
 */
public final class NativeLibraryLoader {

	/**
	 * Dynamic native librarys base path.
	 */
	private final static String BASE_PATH = NativeLibraryLoader.class.getName().replace(".", "/")
			.replace(NativeLibraryLoader.class.getSimpleName(), "") + "natives";

	/**
	 * Loaded state flag.
	 */
	private final static AtomicBoolean LOADED = new AtomicBoolean(false);

	/**
	 * Loading openCv librarys.
	 */
	public final synchronized static void loadLibrarys() {
		if (LOADED.compareAndSet(false, true)) {
			try {
				if (IS_OS_WINDOWS) {
					registerLibrarys0(BASE_PATH + "/windows/" + getArchPath());
				} else {
					registerLibrarys0(BASE_PATH + "/linux/" + getArchPath());
				}
			} catch (Exception e) {
				throw new IllegalStateException(
						String.format("Failed to load OpenCv native library. \ncause at:%s", e.getMessage()), e);
			}
		}
	}

	/**
	 * Register load platform dynamic link librarys.
	 * 
	 * @param path
	 * @throws Exception
	 */
	private final synchronized static void registerLibrarys0(String path) throws Exception {
		URL baseUrl = NativeLibraryLoader.class.getClassLoader().getResource(path);
		if (isNull(baseUrl)) {
			throw new IllegalComponentStateException(String.format("Not found native library for path(%s)", path));
		}
		String[] files = new File(baseUrl.toURI()).list();
		if (isNull(files) || files.length <= 0) {
			throw new IllegalComponentStateException(String.format("Unsupported Current OS arch(%s) of OpenCv-java", OS_ARCH));
		}
		for (String fname : files) {
			System.load(new URL(baseUrl.toString() + "/" + fname).getFile());
		}
	}

	/**
	 * Get OS is arch name as path spec.
	 * 
	 * @return
	 */
	private final static String getArchPath() {
		if (equalsAnyIgnoreCase(OS_ARCH, "x64", "amd64", "x86_64", "ppc64")) {
			return "x64";
		}
		return "x86";
	}

}