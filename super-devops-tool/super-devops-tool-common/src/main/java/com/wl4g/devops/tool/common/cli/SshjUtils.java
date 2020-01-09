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
package com.wl4g.devops.tool.common.cli;

import com.wl4g.devops.tool.common.function.CallbackFunction;
import com.wl4g.devops.tool.common.function.ProcessFunction;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;
import net.schmizz.sshj.xfer.FileSystemFile;
import net.schmizz.sshj.xfer.scp.SCPFileTransfer;
import org.slf4j.Logger;

import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.wl4g.devops.tool.common.io.ByteStreams2.readFullyToString;
import static com.wl4g.devops.tool.common.lang.Assert2.*;
import static com.wl4g.devops.tool.common.log.SmartLoggerFactory.getLogger;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.SystemUtils.USER_HOME;

/**
 * Remote SSH command process tools.
 *
 * @author Wangl.sir <983708408@qq.com>
 * @version v1.0 2019年5月24日
 * @since
 */
public abstract class SshjUtils {
	final protected static Logger log = getLogger(SshjUtils.class);
	final public static int DEFAULT_TRANSFER_BUFFER = 1024 * 6;

	// --- Transfer files. ---

	/**
	 * Transfer get file from remote host.(user sftp)
	 * 
	 * @param host
	 * @param user
	 * @param pemPrivateKey
	 * @param localFile
	 * @param remoteFilePath
	 * @throws Exception
	 */
	public static void scpGetFile(String host, String user, char[] pemPrivateKey, File localFile, String remoteFilePath)
			throws Exception {
		notNull(localFile, "Transfer localFile must not be null.");
		hasText(remoteFilePath, "Transfer remoteDir can't empty.");
		log.debug("SSH2 transfer file from {} to {}@{}:{}", localFile.getAbsolutePath(), user, host, remoteFilePath);

		try {
			// Transfer get file.
			doScpTransfer0(host, user, pemPrivateKey, scp -> {
				scp.download(remoteFilePath, new FileSystemFile(localFile));
			});

			log.debug("SCP get transfered: '{}' from '{}@{}:{}'", localFile.getAbsolutePath(), user, host, remoteFilePath);
		} catch (IOException e) {
			throw e;
		}

	}

	/**
	 * Transfer put file to remote host directory.
	 * 
	 * @param host
	 * @param user
	 * @param pemPrivateKey
	 * @param localFile
	 * @param remoteDir
	 * @throws Exception
	 */
	public static void scpPutFile(String host, String user, char[] pemPrivateKey, File localFile, String remoteDir)
			throws Exception {
		notNull(localFile, "Transfer localFile must not be null.");
		hasText(remoteDir, "Transfer remoteDir can't empty.");
		log.debug("SSH2 transfer file from {} to {}@{}:{}", localFile.getAbsolutePath(), user, host, remoteDir);

		try {
			// Transfer send file.
			doScpTransfer0(host, user, pemPrivateKey, scp -> {
				scp.upload(new FileSystemFile(localFile), remoteDir);
			});

			log.debug("SCP put transfered: '{}' to '{}@{}:{}'", localFile.getAbsolutePath(), user, host, remoteDir);
		} catch (IOException e) {
			throw e;
		}

	}

	/**
	 * Get local current user ssh authentication private key of default.
	 * 
	 * @param host
	 * @param user
	 * @return
	 * @throws Exception
	 */
	private final static char[] getDefaultLocalUserPrivateKey() throws Exception {
		// Check private key.
		File privateKeyFile = new File(USER_HOME + "/.ssh/id_rsa");
		isTrue(privateKeyFile.exists(), String.format("Not found privateKey for %s", privateKeyFile));

		log.warn("Fallback use local user pemPrivateKey of: {}", privateKeyFile);
		try (CharArrayWriter cw = new CharArrayWriter(); FileReader fr = new FileReader(privateKeyFile.getAbsolutePath())) {
			char[] buff = new char[256];
			int len = 0;
			while ((len = fr.read(buff)) != -1) {
				cw.write(buff, 0, len);
			}
			return cw.toCharArray();
		}
	}

	/**
	 * Perform file transfer with remote host, including scp.put/upload or
	 * scp.get/download.
	 * 
	 * @param host
	 * @param user
	 * @param pemPrivateKey
	 * @param processor
	 * @throws IOException
	 */
	private final static void doScpTransfer0(String host, String user, char[] pemPrivateKey,
			CallbackFunction<SCPFileTransfer> processor) throws Exception {
		hasText(host, "Transfer host can't empty.");
		hasText(user, "Transfer user can't empty.");
		notNull(processor, "Transfer processor can't null.");

		// Fallback uses the local current user private key by default.
		if (isNull(pemPrivateKey)) {
			pemPrivateKey = getDefaultLocalUserPrivateKey();
		}
		notNull(pemPrivateKey, "Transfer pemPrivateKey can't null.");

		SSHClient ssh = null;
		SCPFileTransfer scpFileTransfer = null;
		try {
			ssh = new SSHClient();
			ssh.addHostKeyVerifier(new PromiscuousVerifier());
			ssh.connect(host);
			KeyProvider keyProvider = ssh.loadKeys(new String(pemPrivateKey), null, null);
			ssh.authPublickey(user,keyProvider);

			scpFileTransfer = ssh.newSCPFileTransfer();

			// Transfer file(put/get).
			processor.process(scpFileTransfer);
		} catch (Exception e) {
			throw e;
		} finally {
			try {
				if (nonNull(ssh)){
					ssh.disconnect();
					ssh.close();
				}
			} catch (Exception e) {
				log.error("", e);
			}
		}
	}

	// --- Execution commands. ---

	/**
	 * Execution commands with SSH2.
	 * 
	 * @param host
	 * @param user
	 * @param pemPrivateKey
	 * @param command
	 * @param timeoutMs
	 * @return
	 * @throws IOException
	 */
	public static SshExecResponse execWithSsh2(String host, String user, char[] pemPrivateKey, String command, long timeoutMs)
			throws Exception {
		return execWaitForCompleteWithSsh2(host, user, pemPrivateKey, command, cmd -> {
			String message = null, errmsg = null;
			if (nonNull(cmd.getInputStream())) {
				message = readFullyToString(cmd.getInputStream());
			}
			if (nonNull(cmd.getErrorStream())) {
				errmsg = readFullyToString(cmd.getErrorStream());
			}
			return new SshExecResponse(Objects.nonNull(cmd.getExitSignal())?cmd.getExitSignal().toString():null, cmd.getExitStatus(), message, errmsg);
		}, timeoutMs);
	}

	/**
	 * Execution commands wait for complete with SSH2
	 * 
	 * @param host
	 * @param user
	 * @param pemPrivateKey
	 * @param command
	 * @param processor
	 * @param timeoutMs
	 * @return
	 * @throws IOException
	 */
	public static <T> T execWaitForCompleteWithSsh2(String host, String user, char[] pemPrivateKey, String command,
			ProcessFunction<Session.Command, T> processor, long timeoutMs) throws Exception {
		return doExecSsh2Command0(host, user, pemPrivateKey, command, cmd -> {
			// Wait for completed by condition.
			cmd.join(timeoutMs, TimeUnit.MILLISECONDS);
			return processor.process(cmd);
		}, timeoutMs);
	}

	/**
	 * Execution commands with SSH2
	 * 
	 * @param host
	 * @param user
	 * @param pemPrivateKey
	 * @param command
	 * @param processor
	 * @param timeoutMs
	 * @return
	 * @throws IOException
	 */
	private final static <T> T doExecSsh2Command0(String host, String user, char[] pemPrivateKey, String command,
												  ProcessFunction<Session.Command, T> processor, long timeoutMs) throws Exception {
		hasText(host, "SSH2 command host can't empty.");
		hasText(user, "SSH2 command user can't empty.");
		notNull(processor, "SSH2 command processor can't null.");

		// Fallback uses the local current user private key by default.
		if (isNull(pemPrivateKey)) {
			pemPrivateKey = getDefaultLocalUserPrivateKey();
		}
		notNull(pemPrivateKey, "Transfer pemPrivateKey can't null.");

		SSHClient ssh = null;
		Session session = null;
		Session.Command cmd = null;
		try {
			ssh = new SSHClient();
			ssh.addHostKeyVerifier(new PromiscuousVerifier());
			ssh.connect(host);
			KeyProvider keyProvider = ssh.loadKeys(new String(pemPrivateKey), null, null);
			ssh.authPublickey(user,keyProvider);
			session = ssh.startSession();
			//TODO
			String proCommond = "source /etc/profile\nsource /etc/bashrc\n";
			cmd = session.exec(proCommond+command);
			return processor.process(cmd);
		} catch (Exception e) {
			throw e;
		} finally {
			try {
				if (nonNull(session)){
					session.close();
				}
			} catch (Exception e) {
				log.error("", e);
			}
			try {
				if (nonNull(ssh)){
					ssh.disconnect();
					ssh.close();
				}
			} catch (Exception e) {
				log.error("", e);
			}
		}
	}


	public static class SshExecResponse {

		/** Remote commands exit signal. */
		final private String exitSignal;

		/** Remote commands exit code. */
		final private Integer exitCode;

		/** Standard message */
		final private String message;

		/** Error message */
		final private String errmsg;

		public SshExecResponse(String exitSignal, Integer exitCode, String message, String errmsg) {
			super();
			this.exitSignal = exitSignal;
			this.exitCode = exitCode;
			this.message = message;
			this.errmsg = errmsg;
		}

		public String getExitSignal() {
			return exitSignal;
		}

		public Integer getExitCode() {
			return exitCode;
		}

		public String getMessage() {
			return message;
		}

		public String getErrmsg() {
			return errmsg;
		}

	}

}