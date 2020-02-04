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
package com.wl4g.devops.shell.handler;

import com.wl4g.devops.shell.annotation.ShellMethod;
import com.wl4g.devops.shell.annotation.ShellMethod.InterruptType;
import com.wl4g.devops.shell.exception.NoSupportedInterruptShellException;
import com.wl4g.devops.shell.exception.ShellException;
import com.wl4g.devops.shell.handler.EmbeddedServerShellHandler.ServerShellMessageChannel;
import com.wl4g.devops.shell.registry.InternalInjectable;
import com.wl4g.devops.shell.registry.TargetMethodWrapper;
import com.wl4g.devops.shell.signal.BOFStdoutSignal;
import com.wl4g.devops.shell.signal.ChannelState;
import com.wl4g.devops.shell.signal.EOFStdoutSignal;
import com.wl4g.devops.shell.signal.Signal;
import com.wl4g.devops.shell.signal.StderrSignal;
import com.wl4g.devops.shell.signal.StdoutSignal;

import org.slf4j.Logger;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.wl4g.devops.shell.annotation.ShellMethod.InterruptType.*;
import static com.wl4g.devops.shell.signal.ChannelState.*;
import static com.wl4g.devops.tool.common.lang.Assert2.notNull;
import static com.wl4g.devops.tool.common.log.SmartLoggerFactory.getLogger;
import static java.lang.String.format;
import static java.util.Collections.synchronizedMap;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.*;
import static org.apache.commons.lang3.exception.ExceptionUtils.getMessage;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

/**
 * Shell handler context
 *
 * @author wangl.sir
 * @version v1.0 2019年5月24日
 * @since
 */
class ShellContext implements InternalInjectable {
	final public static String DEFAULT_INTERRUPT_LISTENER = "defaultInterruptListener";

	final protected Logger log = getLogger(getClass());

	/**
	 * Event listeners
	 */
	final private Map<String, ShellEventListener> eventListeners = synchronizedMap(new LinkedHashMap<>(4));

	/**
	 * Shell message channel.
	 */
	final private ServerShellMessageChannel channel;

	/**
	 * Line result message state.
	 */
	private ChannelState state = NEW;

	/**
	 * Shell target wrapper object currently executing.
	 * {@link TargetMethodWrapper}
	 */
	private TargetMethodWrapper target;

	ShellContext(ShellContext context) {
		this(context.channel);
		setState(context.state);
		setTarget(context.target);
		// Copy event listeners.
		context.eventListeners.forEach((name, l) -> eventListeners.putIfAbsent(name, l));
	}

	ShellContext(ServerShellMessageChannel channel) {
		notNull(channel, "Shell channel must not be null");
		this.channel = channel;
		// Default listener register.
		eventListeners.putIfAbsent(DEFAULT_INTERRUPT_LISTENER, new ShellEventListener() {
			// Ignore
		});
	}

	ShellContext setState(ChannelState state) {
		notNull(state, "State must not be null");
		this.state = state;
		return this;
	}

	ChannelState getState() {
		return state;
	}

	void setTarget(TargetMethodWrapper target) {
		notNull(target, "Target method must not be null");
		this.target = target;
	}

	TargetMethodWrapper getTarget() {
		if (isNull(target)) {
			throw new Error("The shell target method should not be null???");
		}
		return target;
	}

	/**
	 * Open the channel of the current command line, effect: at this time, the
	 * client console will wait for execution to complete (until the
	 * {@link #completed()} method is called).
	 */
	synchronized ShellContext begin() {
		state = RUNNING;
		// Print begin mark
		printf0(new BOFStdoutSignal());
		return this;
	}

	/**
	 * Complete processing the current command line channel, effect: the client
	 * will reopen the console prompt.</br>
	 * </br>
	 * <b><font color=red>Note: Don't forget to execute it, or the client
	 * console will pause until it timesout.</font><b>
	 */
	public synchronized void completed() {
		state = COMPLETED;
		printf0(new EOFStdoutSignal()); // Ouput end mark
	}

	/**
	 * Are you currently in an interrupt state? (if the current thread does not
	 * open the shell channel, it will return false, that is, uninterrupted)
	 * 
	 * @return
	 * @throws NoSupportedInterruptShellException
	 */
	public final boolean isInterrupted() throws NoSupportedInterruptShellException {
		// Check if the current shell method supports interrupts.
		if (getTarget().getShellMethod().interruptible() == NOT_ALLOW) {
			throw new NoSupportedInterruptShellException(
					format("Interruptible is not supported. You can set @%s(interruptible=%s.%s)",
							ShellMethod.class.getSimpleName(), InterruptType.class.getSimpleName(), ALLOW.name()));
		}
		return nonNull(state) ? (state == INTERRUPTED) : false;
	}

	/**
	 * Get unmodifiable event listeners.
	 * 
	 * @return
	 */
	public Collection<ShellEventListener> getUnmodifiableEventListeners() {
		return unmodifiableCollection(eventListeners.values());
	}

	/**
	 * Add event listener
	 * 
	 * @param name
	 * @param eventListener
	 * @return
	 */
	public boolean addEventListener(String name, ShellEventListener eventListener) {
		Assert.notNull(eventListener, "eventListener must not be null");
		if (nonNull(eventListeners.putIfAbsent(name, eventListener))) {
			throw new ShellException(format("Add an existed event listener: %s", name));
		}
		return eventListeners.get(name) == eventListener;
	}

	/**
	 * Remove event listener
	 * 
	 * @param name
	 * @return
	 */
	public boolean removeEventListener(String name) {
		// Check built-in event listener.
		if (equalsAny(name, DEFAULT_INTERRUPT_LISTENER)) {
			throw new ShellException(format("built-in listener is not allowed to be remove, %s", name));
		}
		return nonNull(eventListeners.remove(name));
	}

	/**
	 * Print message to the client console.
	 *
	 * @param output
	 * @throws IllegalStateException
	 */
	protected ShellContext printf0(Object output) throws IllegalStateException {
		Assert.notNull(output, "Printf message must not be null.");
		Assert.isTrue((output instanceof Signal || output instanceof CharSequence || output instanceof Throwable),
				format("Unsupported print message types: %s", output.getClass()));

		// Check channel state.
		// To solve: com.wl4g.devops.shell.console.ExampleConsole#log3()#MARK1
		// if (getState() != WAITING && !equalsAny(output.toString(), BOF,
		// EOF)) {
		// throw new IllegalStateException("Shell channel is not writable, has
		// it not opened or interrupted/closed?");
		// }

		if (nonNull(channel) && channel.isActive()) {
			try {
				if (output instanceof CharSequence) {
					channel.writeFlush(new StdoutSignal(output.toString()));
				} else if (output instanceof Throwable) {
					channel.writeFlush(new StderrSignal((Throwable) output));
				} else if (output instanceof Signal) {
					channel.writeFlush(output);
				} else {
					throw new ShellException(format("Unsupported printf message type of '%s'", output));
				}
			} catch (IOException e) {
				String errmsg = getRootCauseMessage(e);
				errmsg = isBlank(errmsg) ? getMessage(e) : errmsg;
				log.error("=> {}", errmsg);
			}
		} else {
			throw new IllegalStateException("The current console channel may be closed!");
		}
		return this;
	}

}