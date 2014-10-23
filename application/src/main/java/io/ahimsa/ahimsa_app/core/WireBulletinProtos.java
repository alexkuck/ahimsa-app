package io.ahimsa.ahimsa_app.core;

// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: wirebulletin.proto

public final class WireBulletinProtos {
    private WireBulletinProtos() {}
    public static void registerAllExtensions(
            com.google.protobuf.ExtensionRegistry registry) {
    }
    public interface WireBulletinOrBuilder
            extends com.google.protobuf.MessageOrBuilder {

        // optional string board = 1;
        /**
         * <code>optional string board = 1;</code>
         */
        boolean hasBoard();
        /**
         * <code>optional string board = 1;</code>
         */
        java.lang.String getBoard();
        /**
         * <code>optional string board = 1;</code>
         */
        com.google.protobuf.ByteString
        getBoardBytes();

        // required string message = 2;
        /**
         * <code>required string message = 2;</code>
         */
        boolean hasMessage();
        /**
         * <code>required string message = 2;</code>
         */
        java.lang.String getMessage();
        /**
         * <code>required string message = 2;</code>
         */
        com.google.protobuf.ByteString
        getMessageBytes();

        // optional int64 timestamp = 3;
        /**
         * <code>optional int64 timestamp = 3;</code>
         *
         * <pre>
         * Seconds since 00:00:00 Jan 1, 1970
         * </pre>
         */
        boolean hasTimestamp();
        /**
         * <code>optional int64 timestamp = 3;</code>
         *
         * <pre>
         * Seconds since 00:00:00 Jan 1, 1970
         * </pre>
         */
        long getTimestamp();
    }
    /**
     * Protobuf type {@code WireBulletin}
     */
    public static final class WireBulletin extends
            com.google.protobuf.GeneratedMessage
            implements WireBulletinOrBuilder {
        // Use WireBulletin.newBuilder() to construct.
        private WireBulletin(com.google.protobuf.GeneratedMessage.Builder<?> builder) {
            super(builder);
            this.unknownFields = builder.getUnknownFields();
        }
        private WireBulletin(boolean noInit) { this.unknownFields = com.google.protobuf.UnknownFieldSet.getDefaultInstance(); }

        private static final WireBulletin defaultInstance;
        public static WireBulletin getDefaultInstance() {
            return defaultInstance;
        }

        public WireBulletin getDefaultInstanceForType() {
            return defaultInstance;
        }

        private final com.google.protobuf.UnknownFieldSet unknownFields;
        @java.lang.Override
        public final com.google.protobuf.UnknownFieldSet
        getUnknownFields() {
            return this.unknownFields;
        }
        private WireBulletin(
                com.google.protobuf.CodedInputStream input,
                com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                throws com.google.protobuf.InvalidProtocolBufferException {
            initFields();
            int mutable_bitField0_ = 0;
            com.google.protobuf.UnknownFieldSet.Builder unknownFields =
                    com.google.protobuf.UnknownFieldSet.newBuilder();
            try {
                boolean done = false;
                while (!done) {
                    int tag = input.readTag();
                    switch (tag) {
                        case 0:
                            done = true;
                            break;
                        default: {
                            if (!parseUnknownField(input, unknownFields,
                                    extensionRegistry, tag)) {
                                done = true;
                            }
                            break;
                        }
                        case 10: {
                            bitField0_ |= 0x00000001;
                            board_ = input.readBytes();
                            break;
                        }
                        case 18: {
                            bitField0_ |= 0x00000002;
                            message_ = input.readBytes();
                            break;
                        }
                        case 24: {
                            bitField0_ |= 0x00000004;
                            timestamp_ = input.readInt64();
                            break;
                        }
                    }
                }
            } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                throw e.setUnfinishedMessage(this);
            } catch (java.io.IOException e) {
                throw new com.google.protobuf.InvalidProtocolBufferException(
                        e.getMessage()).setUnfinishedMessage(this);
            } finally {
                this.unknownFields = unknownFields.build();
                makeExtensionsImmutable();
            }
        }
        public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
            return WireBulletinProtos.internal_static_WireBulletin_descriptor;
        }

        protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
        internalGetFieldAccessorTable() {
            return WireBulletinProtos.internal_static_WireBulletin_fieldAccessorTable
                    .ensureFieldAccessorsInitialized(
                            WireBulletinProtos.WireBulletin.class, WireBulletinProtos.WireBulletin.Builder.class);
        }

        public static com.google.protobuf.Parser<WireBulletin> PARSER =
                new com.google.protobuf.AbstractParser<WireBulletin>() {
                    public WireBulletin parsePartialFrom(
                            com.google.protobuf.CodedInputStream input,
                            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                            throws com.google.protobuf.InvalidProtocolBufferException {
                        return new WireBulletin(input, extensionRegistry);
                    }
                };

        @java.lang.Override
        public com.google.protobuf.Parser<WireBulletin> getParserForType() {
            return PARSER;
        }

        private int bitField0_;
        // optional string board = 1;
        public static final int BOARD_FIELD_NUMBER = 1;
        private java.lang.Object board_;
        /**
         * <code>optional string board = 1;</code>
         */
        public boolean hasBoard() {
            return ((bitField0_ & 0x00000001) == 0x00000001);
        }
        /**
         * <code>optional string board = 1;</code>
         */
        public java.lang.String getBoard() {
            java.lang.Object ref = board_;
            if (ref instanceof java.lang.String) {
                return (java.lang.String) ref;
            } else {
                com.google.protobuf.ByteString bs =
                        (com.google.protobuf.ByteString) ref;
                java.lang.String s = bs.toStringUtf8();
                if (bs.isValidUtf8()) {
                    board_ = s;
                }
                return s;
            }
        }
        /**
         * <code>optional string board = 1;</code>
         */
        public com.google.protobuf.ByteString
        getBoardBytes() {
            java.lang.Object ref = board_;
            if (ref instanceof java.lang.String) {
                com.google.protobuf.ByteString b =
                        com.google.protobuf.ByteString.copyFromUtf8(
                                (java.lang.String) ref);
                board_ = b;
                return b;
            } else {
                return (com.google.protobuf.ByteString) ref;
            }
        }

        // required string message = 2;
        public static final int MESSAGE_FIELD_NUMBER = 2;
        private java.lang.Object message_;
        /**
         * <code>required string message = 2;</code>
         */
        public boolean hasMessage() {
            return ((bitField0_ & 0x00000002) == 0x00000002);
        }
        /**
         * <code>required string message = 2;</code>
         */
        public java.lang.String getMessage() {
            java.lang.Object ref = message_;
            if (ref instanceof java.lang.String) {
                return (java.lang.String) ref;
            } else {
                com.google.protobuf.ByteString bs =
                        (com.google.protobuf.ByteString) ref;
                java.lang.String s = bs.toStringUtf8();
                if (bs.isValidUtf8()) {
                    message_ = s;
                }
                return s;
            }
        }
        /**
         * <code>required string message = 2;</code>
         */
        public com.google.protobuf.ByteString
        getMessageBytes() {
            java.lang.Object ref = message_;
            if (ref instanceof java.lang.String) {
                com.google.protobuf.ByteString b =
                        com.google.protobuf.ByteString.copyFromUtf8(
                                (java.lang.String) ref);
                message_ = b;
                return b;
            } else {
                return (com.google.protobuf.ByteString) ref;
            }
        }

        // optional int64 timestamp = 3;
        public static final int TIMESTAMP_FIELD_NUMBER = 3;
        private long timestamp_;
        /**
         * <code>optional int64 timestamp = 3;</code>
         *
         * <pre>
         * Seconds since 00:00:00 Jan 1, 1970
         * </pre>
         */
        public boolean hasTimestamp() {
            return ((bitField0_ & 0x00000004) == 0x00000004);
        }
        /**
         * <code>optional int64 timestamp = 3;</code>
         *
         * <pre>
         * Seconds since 00:00:00 Jan 1, 1970
         * </pre>
         */
        public long getTimestamp() {
            return timestamp_;
        }

        private void initFields() {
            board_ = "";
            message_ = "";
            timestamp_ = 0L;
        }
        private byte memoizedIsInitialized = -1;
        public final boolean isInitialized() {
            byte isInitialized = memoizedIsInitialized;
            if (isInitialized != -1) return isInitialized == 1;

            if (!hasMessage()) {
                memoizedIsInitialized = 0;
                return false;
            }
            memoizedIsInitialized = 1;
            return true;
        }

        public void writeTo(com.google.protobuf.CodedOutputStream output)
                throws java.io.IOException {
            getSerializedSize();
            if (((bitField0_ & 0x00000001) == 0x00000001)) {
                output.writeBytes(1, getBoardBytes());
            }
            if (((bitField0_ & 0x00000002) == 0x00000002)) {
                output.writeBytes(2, getMessageBytes());
            }
            if (((bitField0_ & 0x00000004) == 0x00000004)) {
                output.writeInt64(3, timestamp_);
            }
            getUnknownFields().writeTo(output);
        }

        private int memoizedSerializedSize = -1;
        public int getSerializedSize() {
            int size = memoizedSerializedSize;
            if (size != -1) return size;

            size = 0;
            if (((bitField0_ & 0x00000001) == 0x00000001)) {
                size += com.google.protobuf.CodedOutputStream
                        .computeBytesSize(1, getBoardBytes());
            }
            if (((bitField0_ & 0x00000002) == 0x00000002)) {
                size += com.google.protobuf.CodedOutputStream
                        .computeBytesSize(2, getMessageBytes());
            }
            if (((bitField0_ & 0x00000004) == 0x00000004)) {
                size += com.google.protobuf.CodedOutputStream
                        .computeInt64Size(3, timestamp_);
            }
            size += getUnknownFields().getSerializedSize();
            memoizedSerializedSize = size;
            return size;
        }

        private static final long serialVersionUID = 0L;
        @java.lang.Override
        protected java.lang.Object writeReplace()
                throws java.io.ObjectStreamException {
            return super.writeReplace();
        }

        public static WireBulletinProtos.WireBulletin parseFrom(
                com.google.protobuf.ByteString data)
                throws com.google.protobuf.InvalidProtocolBufferException {
            return PARSER.parseFrom(data);
        }
        public static WireBulletinProtos.WireBulletin parseFrom(
                com.google.protobuf.ByteString data,
                com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                throws com.google.protobuf.InvalidProtocolBufferException {
            return PARSER.parseFrom(data, extensionRegistry);
        }
        public static WireBulletinProtos.WireBulletin parseFrom(byte[] data)
                throws com.google.protobuf.InvalidProtocolBufferException {
            return PARSER.parseFrom(data);
        }
        public static WireBulletinProtos.WireBulletin parseFrom(
                byte[] data,
                com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                throws com.google.protobuf.InvalidProtocolBufferException {
            return PARSER.parseFrom(data, extensionRegistry);
        }
        public static WireBulletinProtos.WireBulletin parseFrom(java.io.InputStream input)
                throws java.io.IOException {
            return PARSER.parseFrom(input);
        }
        public static WireBulletinProtos.WireBulletin parseFrom(
                java.io.InputStream input,
                com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                throws java.io.IOException {
            return PARSER.parseFrom(input, extensionRegistry);
        }
        public static WireBulletinProtos.WireBulletin parseDelimitedFrom(java.io.InputStream input)
                throws java.io.IOException {
            return PARSER.parseDelimitedFrom(input);
        }
        public static WireBulletinProtos.WireBulletin parseDelimitedFrom(
                java.io.InputStream input,
                com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                throws java.io.IOException {
            return PARSER.parseDelimitedFrom(input, extensionRegistry);
        }
        public static WireBulletinProtos.WireBulletin parseFrom(
                com.google.protobuf.CodedInputStream input)
                throws java.io.IOException {
            return PARSER.parseFrom(input);
        }
        public static WireBulletinProtos.WireBulletin parseFrom(
                com.google.protobuf.CodedInputStream input,
                com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                throws java.io.IOException {
            return PARSER.parseFrom(input, extensionRegistry);
        }

        public static Builder newBuilder() { return Builder.create(); }
        public Builder newBuilderForType() { return newBuilder(); }
        public static Builder newBuilder(WireBulletinProtos.WireBulletin prototype) {
            return newBuilder().mergeFrom(prototype);
        }
        public Builder toBuilder() { return newBuilder(this); }

        @java.lang.Override
        protected Builder newBuilderForType(
                com.google.protobuf.GeneratedMessage.BuilderParent parent) {
            Builder builder = new Builder(parent);
            return builder;
        }
        /**
         * Protobuf type {@code WireBulletin}
         */
        public static final class Builder extends
                com.google.protobuf.GeneratedMessage.Builder<Builder>
                implements WireBulletinProtos.WireBulletinOrBuilder {
            public static final com.google.protobuf.Descriptors.Descriptor
            getDescriptor() {
                return WireBulletinProtos.internal_static_WireBulletin_descriptor;
            }

            protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
            internalGetFieldAccessorTable() {
                return WireBulletinProtos.internal_static_WireBulletin_fieldAccessorTable
                        .ensureFieldAccessorsInitialized(
                                WireBulletinProtos.WireBulletin.class, WireBulletinProtos.WireBulletin.Builder.class);
            }

            // Construct using WireBulletinProtos.WireBulletin.newBuilder()
            private Builder() {
                maybeForceBuilderInitialization();
            }

            private Builder(
                    com.google.protobuf.GeneratedMessage.BuilderParent parent) {
                super(parent);
                maybeForceBuilderInitialization();
            }
            private void maybeForceBuilderInitialization() {
                if (com.google.protobuf.GeneratedMessage.alwaysUseFieldBuilders) {
                }
            }
            private static Builder create() {
                return new Builder();
            }

            public Builder clear() {
                super.clear();
                board_ = "";
                bitField0_ = (bitField0_ & ~0x00000001);
                message_ = "";
                bitField0_ = (bitField0_ & ~0x00000002);
                timestamp_ = 0L;
                bitField0_ = (bitField0_ & ~0x00000004);
                return this;
            }

            public Builder clone() {
                return create().mergeFrom(buildPartial());
            }

            public com.google.protobuf.Descriptors.Descriptor
            getDescriptorForType() {
                return WireBulletinProtos.internal_static_WireBulletin_descriptor;
            }

            public WireBulletinProtos.WireBulletin getDefaultInstanceForType() {
                return WireBulletinProtos.WireBulletin.getDefaultInstance();
            }

            public WireBulletinProtos.WireBulletin build() {
                WireBulletinProtos.WireBulletin result = buildPartial();
                if (!result.isInitialized()) {
                    throw newUninitializedMessageException(result);
                }
                return result;
            }

            public WireBulletinProtos.WireBulletin buildPartial() {
                WireBulletinProtos.WireBulletin result = new WireBulletinProtos.WireBulletin(this);
                int from_bitField0_ = bitField0_;
                int to_bitField0_ = 0;
                if (((from_bitField0_ & 0x00000001) == 0x00000001)) {
                    to_bitField0_ |= 0x00000001;
                }
                result.board_ = board_;
                if (((from_bitField0_ & 0x00000002) == 0x00000002)) {
                    to_bitField0_ |= 0x00000002;
                }
                result.message_ = message_;
                if (((from_bitField0_ & 0x00000004) == 0x00000004)) {
                    to_bitField0_ |= 0x00000004;
                }
                result.timestamp_ = timestamp_;
                result.bitField0_ = to_bitField0_;
                onBuilt();
                return result;
            }

            public Builder mergeFrom(com.google.protobuf.Message other) {
                if (other instanceof WireBulletinProtos.WireBulletin) {
                    return mergeFrom((WireBulletinProtos.WireBulletin)other);
                } else {
                    super.mergeFrom(other);
                    return this;
                }
            }

            public Builder mergeFrom(WireBulletinProtos.WireBulletin other) {
                if (other == WireBulletinProtos.WireBulletin.getDefaultInstance()) return this;
                if (other.hasBoard()) {
                    bitField0_ |= 0x00000001;
                    board_ = other.board_;
                    onChanged();
                }
                if (other.hasMessage()) {
                    bitField0_ |= 0x00000002;
                    message_ = other.message_;
                    onChanged();
                }
                if (other.hasTimestamp()) {
                    setTimestamp(other.getTimestamp());
                }
                this.mergeUnknownFields(other.getUnknownFields());
                return this;
            }

            public final boolean isInitialized() {
                if (!hasMessage()) {

                    return false;
                }
                return true;
            }

            public Builder mergeFrom(
                    com.google.protobuf.CodedInputStream input,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws java.io.IOException {
                WireBulletinProtos.WireBulletin parsedMessage = null;
                try {
                    parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
                } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                    parsedMessage = (WireBulletinProtos.WireBulletin) e.getUnfinishedMessage();
                    throw e;
                } finally {
                    if (parsedMessage != null) {
                        mergeFrom(parsedMessage);
                    }
                }
                return this;
            }
            private int bitField0_;

            // optional string board = 1;
            private java.lang.Object board_ = "";
            /**
             * <code>optional string board = 1;</code>
             */
            public boolean hasBoard() {
                return ((bitField0_ & 0x00000001) == 0x00000001);
            }
            /**
             * <code>optional string board = 1;</code>
             */
            public java.lang.String getBoard() {
                java.lang.Object ref = board_;
                if (!(ref instanceof java.lang.String)) {
                    java.lang.String s = ((com.google.protobuf.ByteString) ref)
                            .toStringUtf8();
                    board_ = s;
                    return s;
                } else {
                    return (java.lang.String) ref;
                }
            }
            /**
             * <code>optional string board = 1;</code>
             */
            public com.google.protobuf.ByteString
            getBoardBytes() {
                java.lang.Object ref = board_;
                if (ref instanceof String) {
                    com.google.protobuf.ByteString b =
                            com.google.protobuf.ByteString.copyFromUtf8(
                                    (java.lang.String) ref);
                    board_ = b;
                    return b;
                } else {
                    return (com.google.protobuf.ByteString) ref;
                }
            }
            /**
             * <code>optional string board = 1;</code>
             */
            public Builder setBoard(
                    java.lang.String value) {
                if (value == null) {
                    throw new NullPointerException();
                }
                bitField0_ |= 0x00000001;
                board_ = value;
                onChanged();
                return this;
            }
            /**
             * <code>optional string board = 1;</code>
             */
            public Builder clearBoard() {
                bitField0_ = (bitField0_ & ~0x00000001);
                board_ = getDefaultInstance().getBoard();
                onChanged();
                return this;
            }
            /**
             * <code>optional string board = 1;</code>
             */
            public Builder setBoardBytes(
                    com.google.protobuf.ByteString value) {
                if (value == null) {
                    throw new NullPointerException();
                }
                bitField0_ |= 0x00000001;
                board_ = value;
                onChanged();
                return this;
            }

            // required string message = 2;
            private java.lang.Object message_ = "";
            /**
             * <code>required string message = 2;</code>
             */
            public boolean hasMessage() {
                return ((bitField0_ & 0x00000002) == 0x00000002);
            }
            /**
             * <code>required string message = 2;</code>
             */
            public java.lang.String getMessage() {
                java.lang.Object ref = message_;
                if (!(ref instanceof java.lang.String)) {
                    java.lang.String s = ((com.google.protobuf.ByteString) ref)
                            .toStringUtf8();
                    message_ = s;
                    return s;
                } else {
                    return (java.lang.String) ref;
                }
            }
            /**
             * <code>required string message = 2;</code>
             */
            public com.google.protobuf.ByteString
            getMessageBytes() {
                java.lang.Object ref = message_;
                if (ref instanceof String) {
                    com.google.protobuf.ByteString b =
                            com.google.protobuf.ByteString.copyFromUtf8(
                                    (java.lang.String) ref);
                    message_ = b;
                    return b;
                } else {
                    return (com.google.protobuf.ByteString) ref;
                }
            }
            /**
             * <code>required string message = 2;</code>
             */
            public Builder setMessage(
                    java.lang.String value) {
                if (value == null) {
                    throw new NullPointerException();
                }
                bitField0_ |= 0x00000002;
                message_ = value;
                onChanged();
                return this;
            }
            /**
             * <code>required string message = 2;</code>
             */
            public Builder clearMessage() {
                bitField0_ = (bitField0_ & ~0x00000002);
                message_ = getDefaultInstance().getMessage();
                onChanged();
                return this;
            }
            /**
             * <code>required string message = 2;</code>
             */
            public Builder setMessageBytes(
                    com.google.protobuf.ByteString value) {
                if (value == null) {
                    throw new NullPointerException();
                }
                bitField0_ |= 0x00000002;
                message_ = value;
                onChanged();
                return this;
            }

            // optional int64 timestamp = 3;
            private long timestamp_ ;
            /**
             * <code>optional int64 timestamp = 3;</code>
             *
             * <pre>
             * Seconds since 00:00:00 Jan 1, 1970
             * </pre>
             */
            public boolean hasTimestamp() {
                return ((bitField0_ & 0x00000004) == 0x00000004);
            }
            /**
             * <code>optional int64 timestamp = 3;</code>
             *
             * <pre>
             * Seconds since 00:00:00 Jan 1, 1970
             * </pre>
             */
            public long getTimestamp() {
                return timestamp_;
            }
            /**
             * <code>optional int64 timestamp = 3;</code>
             *
             * <pre>
             * Seconds since 00:00:00 Jan 1, 1970
             * </pre>
             */
            public Builder setTimestamp(long value) {
                bitField0_ |= 0x00000004;
                timestamp_ = value;
                onChanged();
                return this;
            }
            /**
             * <code>optional int64 timestamp = 3;</code>
             *
             * <pre>
             * Seconds since 00:00:00 Jan 1, 1970
             * </pre>
             */
            public Builder clearTimestamp() {
                bitField0_ = (bitField0_ & ~0x00000004);
                timestamp_ = 0L;
                onChanged();
                return this;
            }

            // @@protoc_insertion_point(builder_scope:WireBulletin)
        }

        static {
            defaultInstance = new WireBulletin(true);
            defaultInstance.initFields();
        }

        // @@protoc_insertion_point(class_scope:WireBulletin)
    }

    private static com.google.protobuf.Descriptors.Descriptor
            internal_static_WireBulletin_descriptor;
    private static
    com.google.protobuf.GeneratedMessage.FieldAccessorTable
            internal_static_WireBulletin_fieldAccessorTable;

    public static com.google.protobuf.Descriptors.FileDescriptor
    getDescriptor() {
        return descriptor;
    }
    private static com.google.protobuf.Descriptors.FileDescriptor
            descriptor;
    static {
        java.lang.String[] descriptorData = {
                "\n\022wirebulletin.proto\"A\n\014WireBulletin\022\r\n\005" +
                        "board\030\001 \001(\t\022\017\n\007message\030\002 \002(\t\022\021\n\ttimestam" +
                        "p\030\003 \001(\003B\024B\022WireBulletinProtos"
        };
        com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner =
                new com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner() {
                    public com.google.protobuf.ExtensionRegistry assignDescriptors(
                            com.google.protobuf.Descriptors.FileDescriptor root) {
                        descriptor = root;
                        internal_static_WireBulletin_descriptor =
                                getDescriptor().getMessageTypes().get(0);
                        internal_static_WireBulletin_fieldAccessorTable = new
                                com.google.protobuf.GeneratedMessage.FieldAccessorTable(
                                internal_static_WireBulletin_descriptor,
                                new java.lang.String[] { "Board", "Message", "Timestamp", });
                        return null;
                    }
                };
        com.google.protobuf.Descriptors.FileDescriptor
                .internalBuildGeneratedFileFrom(descriptorData,
                        new com.google.protobuf.Descriptors.FileDescriptor[] {
                        }, assigner);
    }

    // @@protoc_insertion_point(outer_class_scope)
}
