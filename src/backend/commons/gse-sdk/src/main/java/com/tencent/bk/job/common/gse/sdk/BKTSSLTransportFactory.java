/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 *
 * License for BK-JOB蓝鲸智云作业平台:
 * --------------------------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

package com.tencent.bk.job.common.gse.sdk;

import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransportException;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Arrays;

/**
 * 针对Thrift的SSL做免TrustManager验证，主要是因为centos 7.x以上系统，可能存在认证异常 Caused by: sun.security.validator.ValidatorException:
 * KeyUsage does not allow key encipherment at
 * sun.security.validator.EndEntityChecker.checkTLSServer(EndEntityChecker.java:264) at
 * sun.security.validator.EndEntityChecker.check(EndEntityChecker.java:141) at
 * sun.security.validator.Validator.validate(Validator.java:264) at
 * sun.security.ssl.X509TrustManagerImpl.validate(X509TrustManagerImpl.java:324) at
 * sun.security.ssl.X509TrustManagerImpl.checkTrusted(X509TrustManagerImpl.java:229) at
 * sun.security.ssl.X509TrustManagerImpl.checkServerTrusted(X509TrustManagerImpl.java:124) at
 * sun.security.ssl.ClientHandshaker.serverCertificate(ClientHandshaker.java:1496)
 *
 * @version 1.0
 * @time 2017/6/19.
 */
public class BKTSSLTransportFactory {
    private final static X509TrustManager TRUST_MANAGER = new X509TrustManager() {
        @SuppressWarnings("all")
        @Override
        public void checkClientTrusted(X509Certificate[] xcs, String string) {
        }

        @SuppressWarnings("all")
        @Override
        public void checkServerTrusted(X509Certificate[] xcs, String string) {
        }

        @SuppressWarnings("all")
        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    };

    public BKTSSLTransportFactory() {
    }

    public static TServerSocket getServerSocket(int port) throws TTransportException {
        return getServerSocket(port, 0);
    }

    public static TServerSocket getServerSocket(int port, int clientTimeout) throws TTransportException {
        return getServerSocket(port, clientTimeout, false, (InetAddress) null);
    }

    public static TServerSocket getServerSocket(int port, int clientTimeout, boolean clientAuth, InetAddress ifAddress)
        throws TTransportException {
        SSLServerSocketFactory factory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        return createServer(factory, port, clientTimeout, clientAuth, ifAddress, null);
    }

    public static TServerSocket getServerSocket(int port, int clientTimeout, InetAddress ifAddress,
                                                TSSLTransportParameters params) throws TTransportException {
        if (params != null && (params.isKeyStoreSet || params.isTrustStoreSet)) {
            SSLContext ctx = createSSLContext(params);
            return createServer(ctx.getServerSocketFactory(), port, clientTimeout, params.clientAuth, ifAddress,
                params);
        } else {
            throw new TTransportException(
                "Either one of the KeyStore or TrustStore must be set for SSLTransportParameters");
        }
    }

    private static TServerSocket createServer(SSLServerSocketFactory factory, int port, int timeout, boolean clientAuth,
                                              InetAddress ifAddress, TSSLTransportParameters params) throws TTransportException {
        try {
            SSLServerSocket serverSocket = (SSLServerSocket) factory.createServerSocket(port, 100, ifAddress);
            serverSocket.setSoTimeout(timeout);
            serverSocket.setNeedClientAuth(clientAuth);
            if (params != null && params.cipherSuites != null) {
                serverSocket.setEnabledCipherSuites(params.cipherSuites);
            }

            return new TServerSocket(
                (TServerSocket.ServerSocketTransportArgs) (new TServerSocket.ServerSocketTransportArgs())
                    .serverSocket(serverSocket).clientTimeout(timeout));
        } catch (Exception var7) {
            throw new TTransportException("Could not bind to port " + port, var7);
        }
    }

    public static TSocket getClientSocket(String host, int port, int timeout) throws TTransportException {
        SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        return createClient(factory, host, port, timeout);
    }

    public static TSocket getClientSocket(String host, int port) throws TTransportException {
        return getClientSocket(host, port, 0);
    }

    public static TSocket getClientSocket(String host, int port, int timeout, TSSLTransportParameters params)
        throws TTransportException {
        if (params != null && (params.isKeyStoreSet || params.isTrustStoreSet)) {
            SSLContext ctx = createSSLContext(params);
            return createClient(ctx.getSocketFactory(), host, port, timeout);
        } else {
            throw new TTransportException(
                "Either one of the KeyStore or TrustStore must be set for SSLTransportParameters");
        }
    }

    private static SSLContext createSSLContext(TSSLTransportParameters params) throws TTransportException {
        FileInputStream fin = null;
        FileInputStream fis = null;

        SSLContext ctx;
        try {
            ctx = SSLContext.getInstance(params.protocol);
            TrustManagerFactory tmf = null;
            KeyManagerFactory kmf = null;
            KeyStore ks;
            if (params.isTrustStoreSet) {
                tmf = TrustManagerFactory.getInstance(params.trustManagerType);
                ks = KeyStore.getInstance(params.trustStoreType);
                fin = new FileInputStream(params.trustStore);
                ks.load(fin, params.trustPass != null ? params.trustPass.toCharArray() : null);
                tmf.init(ks);
            }

            if (params.isKeyStoreSet) {
                kmf = KeyManagerFactory.getInstance(params.keyManagerType);
                ks = KeyStore.getInstance(params.keyStoreType);
                fis = new FileInputStream(params.keyStore);
                ks.load(fis, params.keyPass.toCharArray());
                kmf.init(ks, params.keyPass.toCharArray());
            }

            if (params.isKeyStoreSet && params.isTrustStoreSet) {
                ctx.init(kmf.getKeyManagers(), new TrustManager[]{TRUST_MANAGER}, (SecureRandom) null);
            } else if (params.isKeyStoreSet) {
                ctx.init(kmf.getKeyManagers(), (TrustManager[]) null, (SecureRandom) null);
            } else {
                ctx.init((KeyManager[]) null, new TrustManager[]{TRUST_MANAGER}, (SecureRandom) null);
            }
        } catch (Exception var17) {
            throw new TTransportException("Error creating the transport", var17);
        } finally {
            if (fin != null) {
                try {
                    fin.close();
                } catch (IOException var16) {
                    var16.printStackTrace();
                }
            }

            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException var15) {
                    var15.printStackTrace();
                }
            }

        }

        return ctx;
    }

    private static TSocket createClient(SSLSocketFactory factory, String host, int port, int timeout)
        throws TTransportException {
        try {
            SSLSocket socket = (SSLSocket) factory.createSocket(host, port);
            socket.setSoTimeout(timeout);
            return new TSocket(socket);
        } catch (Exception var5) {
            throw new TTransportException("Could not connect to " + host + " on port " + port, var5);
        }
    }

    public static class TSSLTransportParameters {
        protected String protocol;
        protected String keyStore;
        protected String keyPass;
        protected String keyManagerType;
        protected String keyStoreType;
        protected String trustStore;
        protected String trustPass;
        protected String trustManagerType;
        protected String trustStoreType;
        protected String[] cipherSuites;
        protected boolean clientAuth;
        protected boolean isKeyStoreSet;
        protected boolean isTrustStoreSet;

        public TSSLTransportParameters() {
            this.protocol = "TLS";
            this.keyManagerType = KeyManagerFactory.getDefaultAlgorithm();
            this.keyStoreType = "JKS";
            this.trustManagerType = TrustManagerFactory.getDefaultAlgorithm();
            this.trustStoreType = "JKS";
            this.clientAuth = false;
            this.isKeyStoreSet = false;
            this.isTrustStoreSet = false;
        }

        public TSSLTransportParameters(String protocol, String[] cipherSuites) {
            this(protocol, cipherSuites, false);
        }

        public TSSLTransportParameters(String protocol, String[] cipherSuites, boolean clientAuth) {
            this.protocol = "TLS";
            this.keyManagerType = KeyManagerFactory.getDefaultAlgorithm();
            this.keyStoreType = "JKS";
            this.trustManagerType = TrustManagerFactory.getDefaultAlgorithm();
            this.trustStoreType = "JKS";
            this.clientAuth = false;
            this.isKeyStoreSet = false;
            this.isTrustStoreSet = false;
            if (protocol != null) {
                this.protocol = protocol;
            }

            this.cipherSuites = (String[]) Arrays.copyOf(cipherSuites, cipherSuites.length);
            this.clientAuth = clientAuth;
        }

        public void setKeyStore(String keyStore, String keyPass, String keyManagerType, String keyStoreType) {
            this.keyStore = keyStore;
            this.keyPass = keyPass;
            if (keyManagerType != null) {
                this.keyManagerType = keyManagerType;
            }

            if (keyStoreType != null) {
                this.keyStoreType = keyStoreType;
            }

            this.isKeyStoreSet = true;
        }

        public void setKeyStore(String keyStore, String keyPass) {
            this.setKeyStore(keyStore, keyPass, (String) null, (String) null);
        }

        public void setTrustStore(String trustStore, String trustPass, String trustManagerType, String trustStoreType) {
            this.trustStore = trustStore;
            this.trustPass = trustPass;
            if (trustManagerType != null) {
                this.trustManagerType = trustManagerType;
            }

            if (trustStoreType != null) {
                this.trustStoreType = trustStoreType;
            }

            this.isTrustStoreSet = true;
        }

        public void setTrustStore(String trustStore, String trustPass) {
            this.setTrustStore(trustStore, trustPass, (String) null, (String) null);
        }

        public void requireClientAuth(boolean clientAuth) {
            this.clientAuth = clientAuth;
        }
    }
}
