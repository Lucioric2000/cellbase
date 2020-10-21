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
import org.opencb.cellbase.core.common.core.Transcript;

/**
 * Tfbs generated by hbm2java.
 */
public class Tfbs implements java.io.Serializable {

    private int tfbsId;
    private Pwm pwm;
    private Transcript transcript;
    private RegulatoryRegion regulatoryRegion;
    private Gene gene;
    private String tfName;
    private String targetGeneName;
    private String chromosome;
    private int start;
    private int end;
    private String strand;
    private int relativeStart;
    private int relativeEnd;
    private double score;
    private String sequence;
    private String cellType;
    private String source;

    public Tfbs() {
    }

    public Tfbs(int tfbsId, Pwm pwm, Transcript transcript,
                RegulatoryRegion regulatoryRegion, Gene gene, String tfName,
                String targetGeneName, String chromosome, int start, int end,
                String strand, int relativeStart, int relativeEnd, double score,
                String sequence, String cellType, String source) {
        this.tfbsId = tfbsId;
        this.pwm = pwm;
        this.transcript = transcript;
        this.regulatoryRegion = regulatoryRegion;
        this.gene = gene;
        this.tfName = tfName;
        this.targetGeneName = targetGeneName;
        this.chromosome = chromosome;
        this.start = start;
        this.end = end;
        this.strand = strand;
        this.relativeStart = relativeStart;
        this.relativeEnd = relativeEnd;
        this.score = score;
        this.sequence = sequence;
        this.cellType = cellType;
        this.source = source;
    }

    public int getTfbsId() {
        return this.tfbsId;
    }

    public void setTfbsId(int tfbsId) {
        this.tfbsId = tfbsId;
    }

    public Pwm getPwm() {
        return this.pwm;
    }

    public void setPwm(Pwm pwm) {
        this.pwm = pwm;
    }

    public Transcript getTranscript() {
        return this.transcript;
    }

    public void setTranscript(Transcript transcript) {
        this.transcript = transcript;
    }

    public RegulatoryRegion getRegulatoryRegion() {
        return this.regulatoryRegion;
    }

    public void setRegulatoryRegion(RegulatoryRegion regulatoryRegion) {
        this.regulatoryRegion = regulatoryRegion;
    }

    public Gene getGene() {
        return this.gene;
    }

    public void setGene(Gene gene) {
        this.gene = gene;
    }

    public String getTfName() {
        return this.tfName;
    }

    public void setTfName(String tfName) {
        this.tfName = tfName;
    }

    public String getTargetGeneName() {
        return this.targetGeneName;
    }

    public void setTargetGeneName(String targetGeneName) {
        this.targetGeneName = targetGeneName;
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

    public int getRelativeStart() {
        return this.relativeStart;
    }

    public void setRelativeStart(int relativeStart) {
        this.relativeStart = relativeStart;
    }

    public int getRelativeEnd() {
        return this.relativeEnd;
    }

    public void setRelativeEnd(int relativeEnd) {
        this.relativeEnd = relativeEnd;
    }

    public double getScore() {
        return this.score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public String getSequence() {
        return this.sequence;
    }

    public void setSequence(String sequence) {
        this.sequence = sequence;
    }

    public String getCellType() {
        return this.cellType;
    }

    public void setCellType(String cellType) {
        this.cellType = cellType;
    }

    public String getSource() {
        return this.source;
    }

    public void setSource(String source) {
        this.source = source;
    }

}
