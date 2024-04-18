/*
 * Copyright (c) 2022 Solana Mobile Inc.
 */

package com.solana.mobilewalletadapter.walletlib.scenario;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.solana.mobilewalletadapter.common.WebSocketsTransportContract;
import com.solana.mobilewalletadapter.common.protocol.SessionProperties;
import com.solana.mobilewalletadapter.walletlib.authorization.AuthIssuerConfig;
import com.solana.mobilewalletadapter.walletlib.protocol.MobileWalletAdapterConfig;
import com.solana.mobilewalletadapter.walletlib.transport.websockets.server.LocalWebSocketServer;

import java.util.List;

public class LocalWebSocketServerScenario extends LocalScenario {
    @WebSocketsTransportContract.LocalPortRange
    public final int port;

    @NonNull
    private final LocalWebSocketServer mWebSocketServer;
    private State mState = State.NOT_STARTED;

    public LocalWebSocketServerScenario(@NonNull Context context,
                                        @NonNull MobileWalletAdapterConfig mobileWalletAdapterConfig,
                                        @NonNull AuthIssuerConfig authIssuerConfig,
                                        @NonNull LocalScenario.Callbacks callbacks,
                                        @NonNull byte[] associationPublicKey,
                                        @WebSocketsTransportContract.LocalPortRange int port) {
        this(context, mobileWalletAdapterConfig, authIssuerConfig, callbacks,
                associationPublicKey, List.of(), port);
    }

    public LocalWebSocketServerScenario(@NonNull Context context,
                                        @NonNull MobileWalletAdapterConfig mobileWalletAdapterConfig,
                                        @NonNull AuthIssuerConfig authIssuerConfig,
                                        @NonNull LocalScenario.Callbacks callbacks,
                                        @NonNull byte[] associationPublicKey,
                                        @NonNull List<SessionProperties.ProtocolVersion> associationProtocolVersions,
                                        @WebSocketsTransportContract.LocalPortRange int port) {
        this(context, mobileWalletAdapterConfig, authIssuerConfig, callbacks,
                associationPublicKey, port, new DevicePowerConfigProvider(context),
                associationProtocolVersions, null);
    }

    public LocalWebSocketServerScenario(@NonNull Context context,
                                        @NonNull MobileWalletAdapterConfig mobileWalletAdapterConfig,
                                        @NonNull AuthIssuerConfig authIssuerConfig,
                                        @NonNull LocalScenario.Callbacks callbacks,
                                        @NonNull byte[] associationPublicKey,
                                        @NonNull List<SessionProperties.ProtocolVersion> associationProtocolVersions,
                                        @WebSocketsTransportContract.LocalPortRange int port,
                                        @Nullable String callingPackage) {
        this(context, mobileWalletAdapterConfig, authIssuerConfig, callbacks,
                associationPublicKey, port, new DevicePowerConfigProvider(context),
                associationProtocolVersions, callingPackage);
    }

    /*package*/ LocalWebSocketServerScenario(@NonNull Context context,
                                             @NonNull MobileWalletAdapterConfig mobileWalletAdapterConfig,
                                             @NonNull AuthIssuerConfig authIssuerConfig,
                                             @NonNull LocalScenario.Callbacks callbacks,
                                             @NonNull byte[] associationPublicKey,
                                             @WebSocketsTransportContract.LocalPortRange int port,
                                             PowerConfigProvider powerConfigProvider,
                                             @NonNull List<SessionProperties.ProtocolVersion> associationProtocolVersions,
                                             @Nullable String callingPackage) {
        super(context, mobileWalletAdapterConfig, authIssuerConfig, callbacks, associationPublicKey,
                powerConfigProvider, associationProtocolVersions, callingPackage);
        this.port = port;
        this.mWebSocketServer = new LocalWebSocketServer(this, mWebSocketServerCallbacks);
    }

    @Override
    public void start() {
        if (mState != State.NOT_STARTED) {
            throw new IllegalStateException("Already started");
        }
        mState = State.RUNNING;
        mServiceBinder.bindService();
        mWebSocketServer.init();
        super.start();
    }

    @Override
    public void close() {
        if (mState == State.CLOSED) {
            return;
        }
        mState = State.CLOSED;
        mIoHandler.post(() -> {
            mCallbacks.onScenarioComplete();
            mWebSocketServer.close(); // this will close all MobileWalletAdapterSessions
            mServiceBinder.unbindService();
            mCallbacks.onScenarioTeardownComplete();
        });
    }

    @NonNull
    private final LocalWebSocketServer.Callbacks mWebSocketServerCallbacks =
            new LocalWebSocketServer.Callbacks() {
        @Override
        public void onStarted() {
            mIoHandler.post(mCallbacks::onScenarioReady);
        }

        @Override
        public void onFatalError() {
            mIoHandler.post(mCallbacks::onScenarioError);
        }
    };

    private enum State {
        NOT_STARTED, RUNNING, CLOSED
    }
}