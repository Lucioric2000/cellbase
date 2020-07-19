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

package org.opencb.cellbase.core.common.regulatory;

// Generated Jun 5, 2012 6:41:13 PM by Hibernate Tools 3.4.0.CR1

import org.opencb.cellbase.core.common.core.Gene;

/**
 * MirnaTarget generated by hbm2java.
 */
public class MirnaTarget implements java.io.Serializable {

    private int mirnaTargetId;
    private Gene gene;
    private MirnaMature mirnaMature;
    private String mirbaseId;
    private String geneTargetName;
    private String experimentalMethod;
    private String chromosome;
    private int start;
    private int end;
    private String strand;
    private double score;
    private String pubmedId;
    private String source;

    public MirnaTarget() {
    }

    public MirnaTarget(int mirnaTargetId, Gene gene, MirnaMature mirnaMature,
                       String mirbaseId, String geneTargetName, String experimentalMethod,
                       String chromosome, int start, int end, String strand, double score,
                       String pubmedId, String source) {
        this.mirnaTargetId = mirnaTargetId;
        this.gene = gene;
        this.mirnaMature = mirnaMature;
        this.mirbaseId = mirbaseId;
        this.geneTargetName = geneTargetName;
        this.experimentalMethod = experimentalMethod;
        this.chromosome = chromosome;
        this.start = start;
        this.end = end;
        this.strand = strand;
        this.score = score;
        this.pubmedId = pubmedId;
        this.source = source;
    }

    public int getMirnaTargetId() {
        return this.mirnaTargetId;
    }

    public void setMirnaTargetId(int mirnaTargetId) {
        this.mirnaTargetId = mirnaTargetId;
    }

    public Gene getGene() {
        return this.gene;
    }

    public void setGene(Gene gene) {
        this.gene = gene;
    }

    public MirnaMature getMirnaMature() {
        return this.mirnaMature;
    }

    public void setMirnaMature(MirnaMature mirnaMature) {
        this.mirnaMature = mirnaMature;
    }

    public String getMirbaseId() {
        return this.mirbaseId;
    }

    public void setMirbaseId(String mirbaseId) {
        this.mirbaseId = mirbaseId;
    }

    public String getGeneTargetName() {
        return this.geneTargetName;
    }

    public void setGeneTargetName(String geneTargetName) {
        this.geneTargetName = geneTargetName;
    }

    public String getExperimentalMethod() {
        return this.experimentalMethod;
    }

    public void setExperimentalMethod(String experimentalMethod) {
        this.experimentalMethod = experimentalMethod;
    }

    public String getChromosome() {
        return this.chromosome;
    }

    public void setChromosome(String chromosome) {
        this.chromosome = chromosome;
    }

    public int getStart() {
        return this.start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getEnd() {
        return this.end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public String getStrand() {
        return this.strand;
    }

    public void setStrand(String strand) {
        this.strand = strand;
    }

    public double getScore() {
        return this.score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public String getPubmedId() {
        return this.pubmedId;
    }

    public void setPubmedId(String pubmedId) {
        this.pubmedId = pubmedId;
    }

    public String getSource() {
        return this.source;
    }

    public void setSource(String source) {
        this.source = source;
    }

}
