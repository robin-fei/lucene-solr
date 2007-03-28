package org.apache.lucene.search.spans;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.search.Weight;

import java.io.IOException;

/**
 * Public for extension only.
 */
public class SpanScorer extends Scorer {
  protected Spans spans;
  protected Weight weight;
  protected byte[] norms;
  protected float value;

  protected boolean firstTime = true;
  protected boolean more = true;

  protected int doc;
  protected float freq;

  protected SpanScorer(Spans spans, Weight weight, Similarity similarity, byte[] norms)
    throws IOException {
    super(similarity);
    this.spans = spans;
    this.norms = norms;
    this.weight = weight;
    this.value = weight.getValue();
    doc = -1;
  }

  public boolean next() throws IOException {
    if (firstTime) {
      more = spans.next();
      firstTime = false;
    }
    return setFreqCurrentDoc();
  }

  public boolean skipTo(int target) throws IOException {
    if (firstTime) {
      more = spans.skipTo(target);
      firstTime = false;
    }
    if (! more) {
      return false;
    }
    if (spans.doc() < target) { // setFreqCurrentDoc() leaves spans.doc() ahead
      more = spans.skipTo(target);
    }
    return setFreqCurrentDoc();
  }

  private boolean setFreqCurrentDoc() throws IOException {
    if (! more) {
      return false;
    }
    doc = spans.doc();
    freq = 0.0f;
    while (more && doc == spans.doc()) {
      int matchLength = spans.end() - spans.start();
      freq += getSimilarity().sloppyFreq(matchLength);
      more = spans.next();
    }
    return more || (freq != 0);
  }

  public int doc() { return doc; }

  public float score() throws IOException {
    float raw = getSimilarity().tf(freq) * value; // raw score
    return raw * Similarity.decodeNorm(norms[doc]); // normalize
  }

  public Explanation explain(final int doc) throws IOException {
    Explanation tfExplanation = new Explanation();

    skipTo(doc);

    float phraseFreq = (doc() == doc) ? freq : 0.0f;
    tfExplanation.setValue(getSimilarity().tf(phraseFreq));
    tfExplanation.setDescription("tf(phraseFreq=" + phraseFreq + ")");

    return tfExplanation;
  }

}
