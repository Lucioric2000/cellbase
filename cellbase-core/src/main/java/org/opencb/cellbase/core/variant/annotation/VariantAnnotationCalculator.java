/*
 * Copyright 2015 OpenCB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opencb.cellbase.core.variant.annotation;

import org.opencb.biodata.models.core.Gene;
import org.opencb.biodata.models.core.Region;
import org.opencb.biodata.models.variant.Variant;
import org.opencb.biodata.models.variant.VariantNormalizer;
import org.opencb.biodata.models.variant.avro.*;
import org.opencb.cellbase.core.api.*;
import org.opencb.biodata.models.core.RegulatoryFeature;
import org.opencb.commons.datastore.core.QueryOptions;
import org.opencb.commons.datastore.core.QueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;

//import org.opencb.cellbase.core.db.api.core.ConservedRegionDBAdaptor;
//import org.opencb.cellbase.core.db.api.core.GeneDBAdaptor;
//import org.opencb.cellbase.core.db.api.core.GenomeDBAdaptor;
//import org.opencb.cellbase.core.db.api.core.ProteinDBAdaptor;
//import org.opencb.cellbase.core.db.api.regulatory.RegulatoryRegionDBAdaptor;
//import org.opencb.cellbase.core.db.api.variation.ClinicalDBAdaptor;
//import org.opencb.cellbase.core.db.api.variation.VariantFunctionalScoreDBAdaptor;
//import org.opencb.cellbase.core.db.api.variation.VariationDBAdaptor;

/**
 * Created by imedina on 06/02/16.
 */
/**
 * Created by imedina on 11/07/14.
 *
 * @author Javier Lopez fjlopez@ebi.ac.uk;
 */
public class VariantAnnotationCalculator { //extends MongoDBAdaptor implements VariantAnnotationDBAdaptor<VariantAnnotation> {

    private GenomeDBAdaptor genomeDBAdaptor;
    private GeneDBAdaptor geneDBAdaptor;
    private RegulationDBAdaptor regulationDBAdaptor;
    private VariantDBAdaptor variantDBAdaptor;
    private ClinicalDBAdaptor clinicalDBAdaptor;
    private ProteinDBAdaptor proteinDBAdaptor;
    private ConservationDBAdaptor conservationDBAdaptor;

    private DBAdaptorFactory dbAdaptorFactory;
    //    private ObjectMapper geneObjectMapper;
    private final VariantNormalizer normalizer;

    private Logger logger = LoggerFactory.getLogger(this.getClass());

//    public VariantAnnotationCalculator(String species, String assembly, MongoDataStore mongoDataStore) {
////        super(species, assembly, mongoDataStore);
//
//        normalizer = new VariantNormalizer(false);
//        logger.debug("VariantAnnotationMongoDBAdaptor: in 'constructor'");
//    }

    public VariantAnnotationCalculator(String species, String assembly, DBAdaptorFactory dbAdaptorFactory) {
        normalizer = new VariantNormalizer(false);

        this.dbAdaptorFactory = dbAdaptorFactory;

        genomeDBAdaptor = dbAdaptorFactory.getGenomeDBAdaptor(species, assembly);
        variantDBAdaptor = dbAdaptorFactory.getVariationDBAdaptor(species, assembly);
        geneDBAdaptor = dbAdaptorFactory.getGeneDBAdaptor(species, assembly);
        regulationDBAdaptor = dbAdaptorFactory.getRegulationDBAdaptor(species, assembly);
        proteinDBAdaptor = dbAdaptorFactory.getProteinDBAdaptor(species, assembly);
        conservationDBAdaptor = dbAdaptorFactory.getConservationDBAdaptor(species, assembly);
        clinicalDBAdaptor = dbAdaptorFactory.getClinicalDBAdaptor(species, assembly);

        logger.debug("VariantAnnotationMongoDBAdaptor: in 'constructor'");
    }

    @Deprecated
    public QueryResult getAllConsequenceTypesByVariant(Variant variant, QueryOptions queryOptions) {
        long dbTimeStart = System.currentTimeMillis();

        // We process include and exclude query options to know which annotators to use.
        // Include parameter has preference over exclude.
        Set<String> annotatorSet = getAnnotatorSet(queryOptions);

        // This field contains all the fields to be returned by overlapping genes
        String includeGeneFields = getIncludedGeneFields(annotatorSet);
        List<Gene> geneList = getAffectedGenes(variant, includeGeneFields);

        // TODO the last 'true' parameter needs to be changed by annotatorSet.contains("regulatory") once is ready
        List<ConsequenceType> consequenceTypeList = getConsequenceTypeList(variant, geneList, true);

        QueryResult queryResult = new QueryResult();
        queryResult.setId(variant.toString());
        queryResult.setDbTime(Long.valueOf(System.currentTimeMillis() - dbTimeStart).intValue());
        queryResult.setNumResults(consequenceTypeList.size());
        queryResult.setNumTotalResults(consequenceTypeList.size());
        queryResult.setResult(consequenceTypeList);

        return queryResult;

    }

    public QueryResult getAnnotationByVariant(Variant variant, QueryOptions queryOptions) {
        return getAnnotationByVariantList(Collections.singletonList(variant), queryOptions).get(0);
    }

    public List<QueryResult<VariantAnnotation>> getAnnotationByVariantList(List<Variant> variantList, QueryOptions queryOptions) {

        List<Variant> normalizedVariantList = normalizer.apply(variantList);

        // We process include and exclude query options to know which annotators to use.
        // Include parameter has preference over exclude.
        Set<String> annotatorSet = getAnnotatorSet(queryOptions);
        logger.debug("Annotators to use: {}", annotatorSet.toString());

        // This field contains all the fields to be returned by overlapping genes
        String includeGeneFields = getIncludedGeneFields(annotatorSet);

        // Object to be returned
        List<QueryResult<VariantAnnotation>> variantAnnotationResultList = new ArrayList<>(normalizedVariantList.size());

        long globalStartTime = System.currentTimeMillis();
        long startTime;
        queryOptions = new QueryOptions();

        /*
         * Next three async blocks calculate annotations using Futures, this will be calculated in a different thread.
         * Once the main loop has finished then they will be stored. This provides a ~30% of performance improvement.
         */
        ExecutorService fixedThreadPool = Executors.newFixedThreadPool(4);
        FutureVariationAnnotator futureVariationAnnotator = null;
        Future<List<QueryResult<Variant>>> variationFuture = null;
        if (annotatorSet.contains("variation") || annotatorSet.contains("populationFrequencies")) {
            futureVariationAnnotator = new FutureVariationAnnotator(normalizedVariantList, queryOptions);
            variationFuture = fixedThreadPool.submit(futureVariationAnnotator);
        }

        FutureConservationAnnotator futureConservationAnnotator = null;
        Future<List<QueryResult>> conservationFuture = null;
        if (annotatorSet.contains("conservation")) {
            futureConservationAnnotator = new FutureConservationAnnotator(normalizedVariantList, queryOptions);
            conservationFuture = fixedThreadPool.submit(futureConservationAnnotator);
        }

        FutureVariantFunctionalScoreAnnotator futureVariantFunctionalScoreAnnotator = null;
        Future<List<QueryResult<Score>>> variantFunctionalScoreFuture = null;
        if (annotatorSet.contains("functionalScore")) {
            futureVariantFunctionalScoreAnnotator = new FutureVariantFunctionalScoreAnnotator(normalizedVariantList, queryOptions);
            variantFunctionalScoreFuture = fixedThreadPool.submit(futureVariantFunctionalScoreAnnotator);
        }

        FutureClinicalAnnotator futureClinicalAnnotator = null;
        Future<List<QueryResult>> clinicalFuture = null;
        if (annotatorSet.contains("clinical")) {
            futureClinicalAnnotator = new FutureClinicalAnnotator(normalizedVariantList, queryOptions);
            clinicalFuture = fixedThreadPool.submit(futureClinicalAnnotator);
        }

        /*
         * We iterate over all variants to get the rest of the annotations and to create the VariantAnnotation objects
         */
        List<Gene> geneList;
        startTime = System.currentTimeMillis();
        for (int i = 0; i < normalizedVariantList.size(); i++) {
            // Fetch overlapping genes for this variant
            geneList = getAffectedGenes(normalizedVariantList.get(i), includeGeneFields);

            // TODO: start & end are both being set to variantList.get(i).getPosition(), modify this for indels
            VariantAnnotation variantAnnotation = new VariantAnnotation();
            variantAnnotation.setChromosome(normalizedVariantList.get(i).getChromosome());
            variantAnnotation.setStart(normalizedVariantList.get(i).getStart());
            variantAnnotation.setReference(normalizedVariantList.get(i).getReference());
            variantAnnotation.setAlternate(normalizedVariantList.get(i).getAlternate());

            if (annotatorSet.contains("consequenceType")) {
                try {
                    List<ConsequenceType> consequenceTypeList = getConsequenceTypeList(normalizedVariantList.get(i), geneList, true);
                    variantAnnotation.setConsequenceTypes(consequenceTypeList);
                    variantAnnotation.setDisplayConsequenceType(getMostSevereConsequenceType(consequenceTypeList));
                } catch (UnsupportedURLVariantFormat e) {
                    logger.error("Consequence type was not calculated for variant {}. Unrecognised variant format.",
                            normalizedVariantList.get(i).toString());
                } catch (Exception e) {
                    logger.error("Unhandled error when calculating consequence type for variant {}",
                            normalizedVariantList.get(i).toString());
                    throw e;
                }
            }

            /*
             * Gene Annotation
             */
            if (annotatorSet.contains("expression")) {
                variantAnnotation.setGeneExpression(new ArrayList<>());
                for (Gene gene : geneList) {
                    if (gene.getAnnotation().getExpression() != null) {
                        variantAnnotation.getGeneExpression().addAll(gene.getAnnotation().getExpression());
                    }
                }
            }

            if (annotatorSet.contains("geneDisease")) {
                variantAnnotation.setGeneTraitAssociation(new ArrayList<>());
                for (Gene gene : geneList) {
                    if (gene.getAnnotation().getDiseases() != null) {
                        variantAnnotation.getGeneTraitAssociation().addAll(gene.getAnnotation().getDiseases());
                    }
                }
            }

            if (annotatorSet.contains("drugInteraction")) {
                variantAnnotation.setGeneDrugInteraction(new ArrayList<>());
                for (Gene gene : geneList) {
                    if (gene.getAnnotation().getDrugs() != null) {
                        variantAnnotation.getGeneDrugInteraction().addAll(gene.getAnnotation().getDrugs());
                    }
                }
            }

            QueryResult queryResult = new QueryResult(normalizedVariantList.get(i).toString());
            queryResult.setDbTime((int) (System.currentTimeMillis() - startTime));
            queryResult.setNumResults(1);
            queryResult.setNumTotalResults(1);
            //noinspection unchecked
            queryResult.setResult(Collections.singletonList(variantAnnotation));

            variantAnnotationResultList.add(queryResult);

        }
        logger.debug("Main loop iteration annotation performance is {}ms for {} variants", System.currentTimeMillis()
                - startTime, normalizedVariantList.size());


        /*
         * Now, hopefully the other annotations have finished and we can store the results.
         * Method 'processResults' has been implemented in the same class for sanity.
         */
        if (futureVariationAnnotator != null) {
            futureVariationAnnotator.processResults(variationFuture, variantAnnotationResultList, annotatorSet);
        }
        if (futureConservationAnnotator != null) {
            futureConservationAnnotator.processResults(conservationFuture, variantAnnotationResultList);
        }
        if (futureVariantFunctionalScoreAnnotator != null) {
            futureVariantFunctionalScoreAnnotator.processResults(variantFunctionalScoreFuture, variantAnnotationResultList);
        }
        if (futureClinicalAnnotator != null) {
            futureClinicalAnnotator.processResults(clinicalFuture, variantAnnotationResultList);
        }
        fixedThreadPool.shutdown();


        logger.debug("Total batch annotation performance is {}ms for {} variants", System.currentTimeMillis()
                - globalStartTime, normalizedVariantList.size());
        return variantAnnotationResultList;
    }

    private String getMostSevereConsequenceType(List<ConsequenceType> consequenceTypeList) {
        int max = -1;
        String mostSevereConsequencetype = null;
        for (ConsequenceType consequenceType : consequenceTypeList) {
            for (SequenceOntologyTerm sequenceOntologyTerm : consequenceType.getSequenceOntologyTerms()) {
                int rank = VariantAnnotationUtils.SO_SEVERITY.get(sequenceOntologyTerm.getName());
                if (rank > max) {
                    max = rank;
                    mostSevereConsequencetype = sequenceOntologyTerm.getName();
                }
            }
        }

        return mostSevereConsequencetype;
    }

    private Set<String> getAnnotatorSet(QueryOptions queryOptions) {
        Set<String> annotatorSet;
        List<String> includeList = queryOptions.getAsStringList("include");
        if (includeList.size() > 0) {
            annotatorSet = new HashSet<>(includeList);
        } else {
            annotatorSet = new HashSet<>(Arrays.asList("variation", "clinical", "conservation", "functionalScore",
                    "consequenceType", "expression", "geneDisease", "drugInteraction", "populationFrequencies"));
            List<String> excludeList = queryOptions.getAsStringList("exclude");
            excludeList.forEach(annotatorSet::remove);
        }
        return annotatorSet;
    }

    private String getIncludedGeneFields(Set<String> annotatorSet) {
        String includeGeneFields = "name,id,start,end,transcripts.id,transcripts.start,transcripts.end,transcripts.strand,"
                + "transcripts.cdsLength,transcripts.annotationFlags,transcripts.biotype,transcripts.genomicCodingStart,"
                + "transcripts.genomicCodingEnd,transcripts.cdnaCodingStart,transcripts.cdnaCodingEnd,transcripts.exons.start,"
                + "transcripts.exons.end,transcripts.exons.sequence,transcripts.exons.phase,mirna.matures,mirna.sequence,"
                + "mirna.matures.cdnaStart,mirna.matures.cdnaEnd";

        if (annotatorSet.contains("expression")) {
            includeGeneFields += ",annotation.expression";
        }
        if (annotatorSet.contains("geneDisease")) {
            includeGeneFields += ",annotation.diseases";
        }
        if (annotatorSet.contains("drugInteraction")) {
            includeGeneFields += ",annotation.drugs";
        }
        return includeGeneFields;
    }

    private List<Gene> getAffectedGenes(Variant variant, String includeFields) {
        int variantStart = variant.getReference().isEmpty() ? variant.getStart() - 1 : variant.getStart();
        QueryOptions queryOptions = new QueryOptions("include", includeFields);
//        QueryResult queryResult = geneDBAdaptor.getAllByRegion(new Region(variant.getChromosome(),
//                variantStart - 5000, variant.getStart() + variant.getReference().length() - 1 + 5000), queryOptions);

        return geneDBAdaptor.getByRegion(
                new Region(variant.getChromosome(), variantStart - 5000, variant.getStart() + variant.getReference().length() - 1 + 5000),
                queryOptions).getResult();

//        return geneDBAdaptor.get(new Query("region", variant.getChromosome()+":"+(variantStart - 5000)+":"
//                +(variant.getStart() + variant.getReference().length() - 1 + 5000)), queryOptions)
//                .getResult();

//        QueryResult queryResult = geneDBAdaptor.getAllByRegion(new Region(variant.getChromosome(),
//                variantStart - 5000, variant.getStart() + variant.getReference().length() - 1 + 5000), queryOptions);
//
//        List<Gene> geneList = new ArrayList<>(queryResult.getNumResults());
//        for (Object object : queryResult.getResult()) {
//            Gene gene = geneObjectMapper.convertValue(object, Gene.class);
//            geneList.add(gene);
//        }
//        return geneList;
    }

    private boolean nonSynonymous(ConsequenceType consequenceType) {
        if (consequenceType.getCodon() == null) {
            return false;
        } else {
            String[] parts = consequenceType.getCodon().split("/");
            String ref = String.valueOf(parts[0]).toUpperCase();
            String alt = String.valueOf(parts[1]).toUpperCase();
            return !VariantAnnotationUtils.IS_SYNONYMOUS_CODON.get(ref).get(alt) && !VariantAnnotationUtils.isStopCodon(ref);
        }
    }

    private ProteinVariantAnnotation getProteinAnnotation(ConsequenceType consequenceType) {
        if (consequenceType.getProteinVariantAnnotation() != null) {
            QueryResult<ProteinVariantAnnotation> proteinVariantAnnotation = proteinDBAdaptor.getVariantAnnotation(
                    consequenceType.getEnsemblTranscriptId(),
                    consequenceType.getProteinVariantAnnotation().getPosition(),
                    consequenceType.getProteinVariantAnnotation().getReference(),
                    consequenceType.getProteinVariantAnnotation().getAlternate(), new QueryOptions());

            if (proteinVariantAnnotation.getNumResults() > 0) {
                return proteinVariantAnnotation.getResult().get(0);
            }
        }
        return null;
    }

    private ConsequenceTypeCalculator getConsequenceTypeCalculator(Variant variant) throws UnsupportedURLVariantFormat {
        if (variant.getReference().isEmpty()) {
            return new ConsequenceTypeInsertionCalculator(genomeDBAdaptor);
        } else {
            if (variant.getAlternate().isEmpty()) {
                return new ConsequenceTypeDeletionCalculator(genomeDBAdaptor);
            } else {
                if (variant.getReference().length() == 1 && variant.getAlternate().length() == 1) {
                    return new ConsequenceTypeSNVCalculator();
                } else {
                    throw new UnsupportedURLVariantFormat();
                }
            }
        }
    }

    private List<RegulatoryFeature> getAffectedRegulatoryRegions(Variant variant) {
        int variantStart = variant.getReference().isEmpty() ? variant.getStart() - 1 : variant.getStart();
        QueryOptions queryOptions = new QueryOptions();
        queryOptions.add("include", "chromosome,start,end");
//        QueryResult queryResult = regulationDBAdaptor.nativeGet(new Query("region", variant.getChromosome()
//                + ":" + variantStart + ":" + (variant.getStart() + variant.getReference().length() - 1)), queryOptions);
        QueryResult<RegulatoryFeature> queryResult = regulationDBAdaptor.getByRegion(new Region(variant.getChromosome(),
                variantStart, variant.getStart() + variant.getReference().length() - 1), queryOptions);

        List<RegulatoryFeature> regionList = new ArrayList<>(queryResult.getNumResults());
        for (RegulatoryFeature object : queryResult.getResult()) {
            regionList.add(object);
        }

//        for (Object object : queryResult.getResult()) {
//            Document dbObject = (Document) object;
//            RegulatoryRegion regulatoryRegion = new RegulatoryRegion();
//            regulatoryRegion.setChromosome((String) dbObject.get("chromosome"));
//            regulatoryRegion.setStart((int) dbObject.get("start"));
//            regulatoryRegion.setEnd((int) dbObject.get("end"));
//            regulatoryRegion.setType((String) dbObject.get("featureType"));
//            regionList.add(regulatoryRegion);
//        }

        return regionList;
    }

    private List<ConsequenceType> getConsequenceTypeList(Variant variant, List<Gene> geneList, boolean regulatoryAnnotation) {
        List<RegulatoryFeature> regulatoryRegionList = null;
        if (regulatoryAnnotation) {
            regulatoryRegionList = getAffectedRegulatoryRegions(variant);
        }
        ConsequenceTypeCalculator consequenceTypeCalculator = getConsequenceTypeCalculator(variant);
        List<ConsequenceType> consequenceTypeList = consequenceTypeCalculator.run(variant, geneList, regulatoryRegionList);
        for (ConsequenceType consequenceType : consequenceTypeList) {
            if (nonSynonymous(consequenceType)) {
                consequenceType.setProteinVariantAnnotation(getProteinAnnotation(consequenceType));
            }
        }
        return consequenceTypeList;
    }

    private List<Region> variantListToRegionList(List<Variant> variantList) {
        List<Region> regionList = new ArrayList<>(variantList.size());
        for (Variant variant : variantList) {
            regionList.add(new Region(variant.getChromosome(), variant.getStart(), variant.getStart()));
        }
        return regionList;
    }

    /*
     * Future classes for Async annotations
     */
    class FutureVariationAnnotator implements Callable<List<QueryResult<Variant>>> {
        private List<Variant> variantList;
        private QueryOptions queryOptions;

        public FutureVariationAnnotator(List<Variant> variantList, QueryOptions queryOptions) {
            this.variantList = variantList;
            this.queryOptions = queryOptions;
        }

        @Override
        public List<QueryResult<Variant>> call() throws Exception {
            long startTime = System.currentTimeMillis();
            List<QueryResult<Variant>> variationQueryResultList = variantDBAdaptor.getByVariant(variantList, queryOptions);
            logger.debug("Variation query performance is {}ms for {} variants", System.currentTimeMillis() - startTime, variantList.size());
            return variationQueryResultList;
        }

        public void processResults(Future<List<QueryResult<Variant>>> conservationFuture,
                                   List<QueryResult<VariantAnnotation>> variantAnnotationResultList, Set<String> annotatorSet) {
            try {
                while (!conservationFuture.isDone()) {
                    Thread.sleep(1);
                }

                List<QueryResult<Variant>> variationQueryResults = conservationFuture.get();
                if (variationQueryResults != null) {
                    for (int i = 0; i < variantAnnotationResultList.size(); i++) {
                        if (variationQueryResults.get(i).first() != null && variationQueryResults.get(i).first().getIds().size() > 0) {
                            variantAnnotationResultList.get(i).first().setId(variationQueryResults.get(i).first().getIds().get(0));

                        }

                        if (annotatorSet.contains("populationFrequencies") && variationQueryResults.get(i).first() != null) {
                            variantAnnotationResultList.get(i).first().setPopulationFrequencies(variationQueryResults.get(i)
                                    .first().getAnnotation().getPopulationFrequencies());
                        }
//                        List<Document> variationDBList = (List<Document>) variationQueryResults.get(i).getResult();
//                        if (variationDBList != null && variationDBList.size() > 0) {
//                            BasicDBList idsDBList = (BasicDBList) variationDBList.get(0).get("ids");
//                            if (idsDBList != null) {
//                                variantAnnotationResultList.get(i).getResult().get(0).setId((String) idsDBList.get(0));
//                            }
//                            if (annotatorSet.contains("populationFrequencies")) {
//                                Document annotationDBObject =  (Document) variationDBList.get(0).get("annotation");
//                                if (annotationDBObject != null) {
//                                    BasicDBList freqsDBList = (BasicDBList) annotationDBObject.get("populationFrequencies");
//                                    if (freqsDBList != null) {
//                                        Document freqDBObject;
//                                        variantAnnotationResultList.get(i).getResult().get(0).setPopulationFrequencies(new ArrayList<>());
//                                        for (int j = 0; j < freqsDBList.size(); j++) {
//                                            freqDBObject = ((Document) freqsDBList.get(j));
//                                            if (freqDBObject != null && freqDBObject.get("refAllele") != null) {
//                                                if (freqDBObject.containsKey("study")) {
//                                                    variantAnnotationResultList.get(i).getResult().get(0)
//                                                            .getPopulationFrequencies()
//                                                            .add(new PopulationFrequency(freqDBObject.get("study").toString(),
//                                                                    freqDBObject.get("population").toString(),
//                                                                    freqDBObject.get("refAllele").toString(),
//                                                                    freqDBObject.get("altAllele").toString(),
//                                                                    Float.valueOf(freqDBObject.get("refAlleleFreq").toString()),
//                                                                    Float.valueOf(freqDBObject.get("altAlleleFreq").toString()),
//                                                                    0.0f, 0.0f, 0.0f));
//                                                } else {
//                                                    variantAnnotationResultList.get(i).getResult().get(0)
//                                                            .getPopulationFrequencies().add(new PopulationFrequency("1000G_PHASE_3",
//                                                            freqDBObject.get("population").toString(),
//                                                            freqDBObject.get("refAllele").toString(),
//                                                            freqDBObject.get("altAllele").toString(),
//                                                            Float.valueOf(freqDBObject.get("refAlleleFreq").toString()),
//                                                            Float.valueOf(freqDBObject.get("altAlleleFreq").toString()),
//                                                            0.0f, 0.0f, 0.0f));
//                                                }
//                                            }
//                                        }
//                                    }
//                                }
//                            }
//                        }
                    }
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    class FutureConservationAnnotator implements Callable<List<QueryResult>> {
        private List<Variant> variantList;

        private QueryOptions queryOptions;

        public FutureConservationAnnotator(List<Variant> variantList, QueryOptions queryOptions) {
            this.variantList = variantList;
            this.queryOptions = queryOptions;
        }

        @Override
        public List<QueryResult> call() throws Exception {
            long startTime = System.currentTimeMillis();
            List<QueryResult> conservationQueryResultList = conservationDBAdaptor
                    .getAllScoresByRegionList(variantListToRegionList(variantList), queryOptions);
            logger.debug("Conservation query performance is {}ms for {} variants", System.currentTimeMillis() - startTime,
                    variantList.size());
            return conservationQueryResultList;
        }

        public void processResults(Future<List<QueryResult>> conservationFuture,
                                   List<QueryResult<VariantAnnotation>> variantAnnotationResultList) {
            try {
                while (!conservationFuture.isDone()) {
                    Thread.sleep(1);
                }

                List<QueryResult> conservationQueryResults = conservationFuture.get();
                if (conservationQueryResults != null) {
                    for (int i = 0; i < variantAnnotationResultList.size(); i++) {
                        variantAnnotationResultList.get(i).getResult().get(0)
                                .setConservation((List<Score>) conservationQueryResults.get(i).getResult());
                    }
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

    }

    class FutureVariantFunctionalScoreAnnotator implements Callable<List<QueryResult<Score>>> {
        private List<Variant> variantList;

        private QueryOptions queryOptions;

        public FutureVariantFunctionalScoreAnnotator(List<Variant> variantList, QueryOptions queryOptions) {
            this.variantList = variantList;
            this.queryOptions = queryOptions;
        }

        @Override
        public List<QueryResult<Score>> call() throws Exception {
            long startTime = System.currentTimeMillis();
//            List<QueryResult> variantFunctionalScoreQueryResultList =
//                    variantFunctionalScoreDBAdaptor.getAllByVariantList(variantList, queryOptions);
            List<QueryResult<Score>> variantFunctionalScoreQueryResultList =
                    variantDBAdaptor.getFunctionalScoreVariant(variantList, queryOptions);
            logger.debug("VariantFunctionalScore query performance is {}ms for {} variants",
                    System.currentTimeMillis() - startTime, variantList.size());
            return variantFunctionalScoreQueryResultList;
        }

        public void processResults(Future<List<QueryResult<Score>>> variantFunctionalScoreFuture,
                                   List<QueryResult<VariantAnnotation>> variantAnnotationResultList) {
            try {
                while (!variantFunctionalScoreFuture.isDone()) {
                    Thread.sleep(1);
                }

                List<QueryResult<Score>> variantFunctionalScoreQueryResults = variantFunctionalScoreFuture.get();
                if (variantFunctionalScoreQueryResults != null) {
                    for (int i = 0; i < variantAnnotationResultList.size(); i++) {
                        variantAnnotationResultList.get(i).getResult().get(0)
                                .setFunctionalScore((List<Score>) variantFunctionalScoreQueryResults.get(i).getResult());
                    }
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

    }

    class FutureClinicalAnnotator implements Callable<List<QueryResult>> {
        private List<Variant> variantList;
        private QueryOptions queryOptions;

        public FutureClinicalAnnotator(List<Variant> variantList, QueryOptions queryOptions) {
            this.variantList = variantList;
            this.queryOptions = queryOptions;
        }

        @Override
        public List<QueryResult> call() throws Exception {
            long startTime = System.currentTimeMillis();
            List<QueryResult> clinicalQueryResultList = clinicalDBAdaptor.getAllByGenomicVariantList(variantList, queryOptions);
            logger.debug("Clinical query performance is {}ms for {} variants", System.currentTimeMillis() - startTime, variantList.size());
            return clinicalQueryResultList;
        }

        public void processResults(Future<List<QueryResult>> clinicalFuture,
                                   List<QueryResult<VariantAnnotation>> variantAnnotationResults) {
            try {
                while (!clinicalFuture.isDone()) {
                    Thread.sleep(1);
                }

                List<QueryResult> clinicalQueryResults = clinicalFuture.get();
                if (clinicalQueryResults != null) {
                    for (int i = 0; i < variantAnnotationResults.size(); i++) {
                        QueryResult clinicalQueryResult = clinicalQueryResults.get(i);
                        if (clinicalQueryResult.getResult() != null && clinicalQueryResult.getResult().size() > 0) {
                            variantAnnotationResults.get(i).getResult().get(0)
                                    .setVariantTraitAssociation((VariantTraitAssociation) clinicalQueryResult.getResult().get(0));
                        }
                    }
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

}