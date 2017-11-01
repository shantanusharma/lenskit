/*
 * LensKit, an open-source toolkit for recommender systems.
 * Copyright 2014-2017 LensKit contributors (see CONTRIBUTORS.md)
 * Copyright 2010-2014 Regents of the University of Minnesota
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.lenskit.pf;

import org.lenskit.inject.Shareable;
import org.lenskit.mf.funksvd.FeatureCount;

import javax.annotation.concurrent.Immutable;
import javax.inject.Inject;
import java.io.Serializable;

/**
 * Hyper-parameters of poisson factorization
 *
 */
@Shareable
@Immutable
public final class PFHyperParameters implements Serializable {
    private static final long serialVersionUID = 1L;

    private final double userWeightShpPrior;
    private final double userActivityShpPrior;
    private final double userActivityPriorMean;
    private final double itemWeightShpPrior;
    private final double itemActivityShpPrior;
    private final double itemActivityPriorMean;
    private final int featureCount;


    @Inject
    public PFHyperParameters(@UserWeightShpPrior double a,
                             @UserActivityShpPrior double aP,
                             @UserActivityPriorMean double bP,
                             @ItemWeightShpPrior double c,
                             @ItemActivityShpPrior double cP,
                             @ItemActivityPriorMean double dP,
                             @FeatureCount int k) {
        userWeightShpPrior = a;
        userActivityShpPrior = aP;
        userActivityPriorMean = bP;
        itemWeightShpPrior = c;
        itemActivityShpPrior = cP;
        itemActivityPriorMean = dP;
        featureCount = k;
    }

    public double getUserWeightShpPrior() {
        return userWeightShpPrior;
    }

    public double getUserActivityShpPrior() {
        return userActivityShpPrior;
    }


    public double getUserActivityPriorMean() {
        return userActivityPriorMean;
    }

    public double getItemWeightShpPrior() {
        return itemWeightShpPrior;
    }

    public double getItemActivityShpPrior() {
        return itemActivityShpPrior;
    }

    public double getItemActivityPriorMean() {
        return itemActivityPriorMean;
    }

    public int getFeatureCount() {
        return featureCount;
    }
}
