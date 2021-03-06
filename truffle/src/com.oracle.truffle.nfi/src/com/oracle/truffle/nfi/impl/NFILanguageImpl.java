/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oracle.truffle.nfi.impl;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.TruffleLanguage.ContextPolicy;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.nfi.impl.LibFFIType.CachedTypeInfo;
import com.oracle.truffle.nfi.spi.NFIBackend;
import com.oracle.truffle.nfi.spi.NFIBackendFactory;
import com.oracle.truffle.nfi.spi.NFIBackendTools;
import com.oracle.truffle.nfi.spi.types.NativeSimpleType;

@TruffleLanguage.Registration(id = "internal/nfi-native", name = "nfi-native", version = "0.1", characterMimeTypes = NFILanguageImpl.MIME_TYPE, internal = true, services = NFIBackendFactory.class, contextPolicy = ContextPolicy.SHARED)
public class NFILanguageImpl extends TruffleLanguage<NFIContext> {

    public static final String MIME_TYPE = "trufflenfi/native";

    private final Assumption singleContextAssumption = Truffle.getRuntime().createAssumption("libffi backend single context");

    static NFILanguageImpl getCurrentLanguage() {
        return getCurrentLanguage(NFILanguageImpl.class);
    }

    static Assumption getSingleContextAssumption() {
        return getCurrentLanguage().singleContextAssumption;
    }

    @Override
    protected void initializeMultipleContexts() {
        super.initializeMultipleContexts();
        singleContextAssumption.invalidate();
    }

    @CompilationFinal private NFIBackendImpl backend;

    @CompilationFinal(dimensions = 1) final CachedTypeInfo[] simpleTypeMap = new CachedTypeInfo[NativeSimpleType.values().length];
    @CompilationFinal(dimensions = 1) final CachedTypeInfo[] arrayTypeMap = new CachedTypeInfo[NativeSimpleType.values().length];
    @CompilationFinal CachedTypeInfo cachedEnvType;

    CachedTypeInfo lookupSimpleTypeInfo(NativeSimpleType type) {
        return simpleTypeMap[type.ordinal()];
    }

    NFIBackendTools getTools() {
        return backend.tools;
    }

    @Override
    protected NFIContext createContext(Env env) {
        env.registerService(new NFIBackendFactory() {

            @Override
            public String getBackendId() {
                return "native";
            }

            @Override
            public NFIBackend createBackend(NFIBackendTools tools) {
                if (backend == null) {
                    /*
                     * Make sure there is exactly one backend instance per engine. That way we can
                     * use identity equality on the backend object for caching decisions.
                     */
                    backend = new NFIBackendImpl(com.oracle.truffle.nfi.impl.NFILanguageImpl.this, tools);
                }
                return backend;
            }
        });
        return new NFIContext(this, env);
    }

    @Override
    protected void initializeContext(NFIContext context) throws Exception {
        context.initialize();
    }

    @Override
    protected boolean patchContext(NFIContext context, Env newEnv) {
        context.patchEnv(newEnv);
        context.initialize();
        return true;
    }

    @Override
    protected void disposeContext(NFIContext context) {
        context.dispose();
    }

    @Override
    protected boolean isThreadAccessAllowed(Thread thread, boolean singleThreaded) {
        // the NFI is fully thread-safe
        return true;
    }

    @Override
    protected CallTarget parse(ParsingRequest request) throws Exception {
        return Truffle.getRuntime().createCallTarget(new RootNode(this) {

            @Override
            public Object execute(VirtualFrame frame) {
                CompilerDirectives.transferToInterpreter();
                throw new UnsupportedOperationException("illegal access to internal language");
            }
        });
    }

    protected static NFIContext getCurrentContext() {
        return getCurrentContext(NFILanguageImpl.class);
    }
}
