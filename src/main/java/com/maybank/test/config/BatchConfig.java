package com.maybank.test.config;

import com.maybank.test.entity.TransactionRecord;
import com.maybank.test.repository.TransactionRecordRepository;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.batch.item.file.transform.LineTokenizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.format.DateTimeFormatter;

@Configuration
public class BatchConfig {

    @Bean
    public FormattingConversionService conversionService() {
        DefaultFormattingConversionService conversionService = new DefaultFormattingConversionService();
        DateTimeFormatterRegistrar registrar = new DateTimeFormatterRegistrar();
        registrar.setDateFormatter(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        registrar.setTimeFormatter(DateTimeFormatter.ofPattern("HH:mm:ss"));
        registrar.registerFormatters(conversionService);
        return conversionService;
    }

    @Bean
    public FlatFileItemReader<TransactionRecord> reader() {
        return new FlatFileItemReaderBuilder<TransactionRecord>()
                .name("transactionItemReader")
                .resource(new ClassPathResource("transactions.txt"))
                .linesToSkip(1)
                .lineTokenizer(new SafePipeDelimitedLineTokenizer())
                .fieldSetMapper(new BeanWrapperFieldSetMapper<>() {{
                    setTargetType(TransactionRecord.class);
                    setConversionService(conversionService());
                }})
                .strict(false)
                .saveState(false)
                .build();
    }

    // Custom tokenizer that safely handles empty lines
    private static class SafePipeDelimitedLineTokenizer implements LineTokenizer {
        private final DelimitedLineTokenizer delegate = new DelimitedLineTokenizer("|");

        public SafePipeDelimitedLineTokenizer() {
            delegate.setNames("accountNumber", "trxAmount", "description", "trxDate", "trxTime", "customerId");
            delegate.setStrict(false); // Don't fail if fewer tokens than expected
        }

        @Override
        public FieldSet tokenize(String line) {
            if (line == null || line.trim().isEmpty()) {
                return null; // Will be filtered by processor
            }
            return delegate.tokenize(line);
        }
    }

    @Bean
    public ItemProcessor<TransactionRecord, TransactionRecord> processor() {
        return record -> {
            // Skip null records from empty lines
            if (record == null) {
                return null;
            }

            // Validate required fields
            if (record.getAccountNumber() == null ||
                    record.getTrxAmount() == null ||
                    record.getTrxDate() == null) {
                System.out.println("Skipping incomplete record: " + record);
                return null;
            }

            return record;
        };
    }

    @Bean
    public ItemWriter<TransactionRecord> writer(TransactionRecordRepository repository) {
        return items -> {
            if (!items.isEmpty()) {
                repository.saveAll(items);
            }
        };
    }

    @Bean
    public Step step(JobRepository jobRepository,
                     PlatformTransactionManager transactionManager,
                     ItemReader<TransactionRecord> reader,
                     ItemProcessor<TransactionRecord, TransactionRecord> processor,
                     ItemWriter<TransactionRecord> writer) {
        return new StepBuilder("transactionProcessingStep", jobRepository)
                .<TransactionRecord, TransactionRecord>chunk(10, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .faultTolerant()
                .skip(Exception.class) // Handle any unexpected errors
                .skipLimit(100) // Allow skipping up to 100 bad records
                .build();
    }

    @Bean
    public Job importTransactionsJob(JobRepository jobRepository, Step step) {
        return new JobBuilder("importTransactionsJob", jobRepository)
                .start(step)
                .build();
    }
}