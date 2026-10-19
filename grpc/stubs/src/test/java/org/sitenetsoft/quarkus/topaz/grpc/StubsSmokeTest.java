package org.sitenetsoft.quarkus.topaz.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import aserto.authorizer.v2.AuthorizerGrpc;
import aserto.authorizer.v2.AuthorizerOuterClass.IsRequest;
import aserto.authorizer.v2.MutinyAuthorizerGrpc;
import aserto.authorizer.v2.api.PolicyContextOuterClass.PolicyContext;
import aserto.directory.exporter.v3.ExporterGrpc;
import aserto.directory.exporter.v3.MutinyExporterGrpc;
import aserto.directory.importer.v3.ImporterGrpc;
import aserto.directory.importer.v3.MutinyImporterGrpc;
import aserto.directory.model.v3.ModelGrpc;
import aserto.directory.model.v3.MutinyModelGrpc;
import aserto.directory.reader.v3.ReaderGrpc;
import aserto.directory.reader.v3.MutinyReaderGrpc;
import aserto.directory.writer.v3.WriterGrpc;
import aserto.directory.writer.v3.MutinyWriterGrpc;

class StubsSmokeTest {
    @Test
    void authorizerStubsExist() {
        assertThat(AuthorizerGrpc.SERVICE_NAME).isEqualTo("aserto.authorizer.v2.Authorizer");
        assertThat(IsRequest.newBuilder().setPolicyContext(
                PolicyContext.newBuilder().setPath("todoApp.GET.todos").addDecisions("allowed")).build()
                .getPolicyContext().getDecisions(0)).isEqualTo("allowed");
    }

    @Test
    void directoryServicesExist() {
        assertThat(List.of(ReaderGrpc.SERVICE_NAME, WriterGrpc.SERVICE_NAME, ModelGrpc.SERVICE_NAME,
                ImporterGrpc.SERVICE_NAME, ExporterGrpc.SERVICE_NAME))
                .containsExactly("aserto.directory.reader.v3.Reader", "aserto.directory.writer.v3.Writer",
                        "aserto.directory.model.v3.Model", "aserto.directory.importer.v3.Importer",
                        "aserto.directory.exporter.v3.Exporter");
    }

    @Test
    void mutinyStubsExist() {
        assertThat(MutinyAuthorizerGrpc.class).isNotNull();
        assertThat(MutinyReaderGrpc.class).isNotNull();
        assertThat(MutinyWriterGrpc.class).isNotNull();
        assertThat(MutinyModelGrpc.class).isNotNull();
        assertThat(MutinyImporterGrpc.class).isNotNull();
        assertThat(MutinyExporterGrpc.class).isNotNull();
    }

    @Test
    void noV2Directory() {
        assertThatThrownBy(() -> Class.forName("aserto.directory.reader.v2.ReaderGrpc"))
                .isInstanceOf(ClassNotFoundException.class);
    }
}
