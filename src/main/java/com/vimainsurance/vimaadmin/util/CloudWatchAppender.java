package com.vimainsurance.vimaadmin.util;

import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.classic.spi.ILoggingEvent;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClientBuilder;
import software.amazon.awssdk.services.cloudwatchlogs.model.*;

import java.time.Instant;
import java.util.*;

public class CloudWatchAppender extends AppenderBase<ILoggingEvent> {

    private CloudWatchLogsClient client;
    private String logGroupName;
    private String logStreamName;
    private String region;
    private String sequenceToken;
    private ch.qos.logback.core.Layout<ILoggingEvent> layout;
    private String accessKeyId;
    private String secretKey;

    @Override
    public void start() {
        CloudWatchLogsClientBuilder builder = CloudWatchLogsClient.builder()
                .region(Region.of(region));

        if (accessKeyId != null && !accessKeyId.isEmpty() && secretKey != null && !secretKey.isEmpty()) {
            AwsBasicCredentials awsCreds = AwsBasicCredentials.create(accessKeyId, secretKey);
            builder.credentialsProvider(StaticCredentialsProvider.create(awsCreds));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        this.client = builder.build();

        ensureLogGroup();
        ensureLogStream();
        fetchSequenceToken();

        super.start();
    }

    @Override
    public void stop() {
        if (client != null) {
            client.close();
        }
        super.stop();
    }

    @Override
    protected void append(ILoggingEvent event) {
        String message = this.layout != null ? this.layout.doLayout(event) : event.getFormattedMessage();

        InputLogEvent logEvent = InputLogEvent.builder()
                .message(message)
                .timestamp(Instant.now().toEpochMilli())
                .build();

        PutLogEventsRequest request = PutLogEventsRequest.builder()
                .logGroupName(logGroupName)
                .logStreamName(logStreamName)
                .logEvents(Collections.singletonList(logEvent))
                .sequenceToken(sequenceToken)
                .build();

        try {
            PutLogEventsResponse response = client.putLogEvents(request);
            sequenceToken = response.nextSequenceToken();
        } catch (InvalidSequenceTokenException ex) {
            sequenceToken = ex.expectedSequenceToken();
        } catch (Exception ex) {
            addError("Failed to send log to CloudWatch", ex);
        }
    }

    private void ensureLogGroup() {
        try {
            client.createLogGroup(CreateLogGroupRequest.builder().logGroupName(logGroupName).build());
        } catch (ResourceAlreadyExistsException ignored) {
        }
    }

    private void ensureLogStream() {
        try {
            client.createLogStream(CreateLogStreamRequest.builder()
                    .logGroupName(logGroupName)
                    .logStreamName(logStreamName)
                    .build());
        } catch (ResourceAlreadyExistsException ignored) {
        }
    }

    private void fetchSequenceToken() {
        DescribeLogStreamsResponse response = client.describeLogStreams(DescribeLogStreamsRequest.builder()
                .logGroupName(logGroupName)
                .logStreamNamePrefix(logStreamName)
                .build());

        if (!response.logStreams().isEmpty()) {
            sequenceToken = response.logStreams().get(0).uploadSequenceToken();
        }
    }

    // Setters for logback.xml

    public void setLogGroupName(String logGroupName) {
        this.logGroupName = logGroupName;
    }

    public void setLogStreamName(String logStreamName) {
        this.logStreamName = logStreamName;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public void setLayout(ch.qos.logback.core.Layout<ILoggingEvent> layout) {
        this.layout = layout;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }
} 