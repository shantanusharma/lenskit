/*
 * LensKit, an open source recommender systems toolkit.
 * Copyright 2010-2016 LensKit Contributors.  See CONTRIBUTORS.md.
 * Work on LensKit has been funded by the National Science Foundation under
 * grants IIS 05-34939, 08-08692, 08-12148, and 10-17697.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.lenskit.pf;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.Stopwatch;
import org.lenskit.data.dao.DataAccessObject;
import org.lenskit.data.dao.file.StaticDataSource;
import org.lenskit.data.entities.EntityFactory;
import org.lenskit.data.ratings.*;
import org.lenskit.util.keys.KeyIndex;

import java.util.*;

import static java.util.stream.Collectors.groupingBy;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.*;


public class RandomDataSplitStrategyProviderTest {
    private Long2ObjectMap<Long2DoubleMap> data;
    private RatingMatrix snapshot;

    @Before
    public void setUp() throws Exception {
        List<Rating> rs = new ArrayList<>();
        double[][] ratings = {
                {0, 12, 13, 14, 15},
                {21, 22, 23, 24, 25},
                {31, 32, 33, 34, 35},
                {41, 42, 43, 44, 45},
                {51, 52, 53, 54, 55},
                {61, 62, 63, 64, 65}};
        EntityFactory ef = new EntityFactory();
        data = new Long2ObjectOpenHashMap<>();
        for (int user = 1; user <= ratings.length; user++) {
            double[] userRatings = ratings[user-1];
            for (int item = 1; item <= userRatings.length; item++) {
                double rating = userRatings[item-1];
                rs.add(ef.rating(user, item, rating));
                Long2DoubleMap itemRatings = data.get(item);
                if (itemRatings == null) itemRatings = new Long2DoubleOpenHashMap();
                itemRatings.put(user, rating);
                data.put(item, itemRatings);
            }
        }

        StaticDataSource source = StaticDataSource.fromList(rs);
        DataAccessObject dao = source.get();
        snapshot = new PackedRatingMatrixProvider(new StandardRatingVectorPDAO(dao), new Random()).get();
    }


    @Test
    public void testDataSize() {
        RandomDataSplitStrategyProvider splitData = new RandomDataSplitStrategyProvider(snapshot, new Random(), 0, 0.1);
        DataSplitStrategy splitStrategy = splitData.get();
        List<RatingMatrixEntry> validations = splitStrategy.getValidationRatings();
        List<RatingMatrixEntry> trainingRaings = splitStrategy.getTrainingMatrix();
        assertThat(validations.size(), equalTo(3));
        int trainingSize = trainingRaings.size();
        assertThat(trainingSize, equalTo(27));
    }

    @Test
    public void testGetRatings() throws Exception {
        RandomDataSplitStrategyProvider splitData = new RandomDataSplitStrategyProvider(snapshot, new Random(), 0, 0.2);
        DataSplitStrategy splitStrategy = splitData.get();
        KeyIndex userIndex = splitStrategy.getUserIndex();
        KeyIndex itemIndex = splitStrategy.getItemIndex();
        List<RatingMatrixEntry> validations = splitStrategy.getValidationRatings();
        List<RatingMatrixEntry> trainingRatings = splitStrategy.getTrainingMatrix();

        for (RatingMatrixEntry re : trainingRatings) {
            int user = re.getUserIndex();
            int item = re.getItemIndex();
            long userId = userIndex.getKey(user);
            long itemId = itemIndex.getKey(item);
            double rating = re.getValue();
            assertThat(rating, equalTo(snapshot.getUserRatingVector(userId).get(itemId)));
            assertThat(rating, equalTo(data.get(itemId).get(userId)));

        }

        for (RatingMatrixEntry re : validations) {
            long userId = re.getUserId();
            long itemId = re.getItemId();
            double rating = re.getValue();
            assertThat(rating, equalTo(snapshot.getUserRatingVector(userId).get(itemId)));
            assertThat(rating, equalTo(data.get(itemId).get(userId)));
        }

    }


    @Test
    public void testGetIndex() {
        RandomDataSplitStrategyProvider splitData = new RandomDataSplitStrategyProvider(snapshot, new Random(), 0, 0.1);
        DataSplitStrategy splitStrategy = splitData.get();
        KeyIndex userIndex = splitStrategy.getUserIndex();
        KeyIndex itemIndex = splitStrategy.getItemIndex();
        List<RatingMatrixEntry> validations = splitStrategy.getValidationRatings();
        List<RatingMatrixEntry> trainingRatings = splitStrategy.getTrainingMatrix();

        for (RatingMatrixEntry re : validations) {
            int user = re.getUserIndex();
            int item = re.getItemIndex();
            long userId = re.getUserId();
            long itemId = re.getItemId();
            assertThat(user, equalTo(userIndex.tryGetIndex(userId)));
            assertThat(item, equalTo(itemIndex.tryGetIndex(itemId)));
        }


        KeyIndex snapshotUserIndex = snapshot.userIndex();
        KeyIndex snapshotItemIndex = snapshot.itemIndex();
        for (RatingMatrixEntry re : trainingRatings) {
            int user = re.getUserIndex();
            int item = re.getItemIndex();
            long userId = userIndex.getKey(user);
            long itemId = itemIndex.getKey(item);
            assertThat(user, equalTo(snapshotUserIndex.tryGetIndex(userId)));
            assertThat(item, equalTo(snapshotItemIndex.tryGetIndex(itemId)));

        }


        int size = snapshotUserIndex.getUpperBound();
        for (int i = 0; i < size; i++) {
            assertThat(snapshotUserIndex.getKey(i), equalTo(userIndex.getKey(i)));
        }

        size = snapshotItemIndex.getUpperBound();
        for (int i = 0; i < size; i++) {
            assertThat(snapshotItemIndex.getKey(i), equalTo(itemIndex.getKey(i)));
        }
    }

    @Test
    public void testStreamGroupby() {
//        RandomDataSplitStrategyProvider splitData = new RandomDataSplitStrategyProvider(snapshot, new Random(), 0, 0.1);
//        DataSplitStrategy splitStrategy = splitData.get();
//        List<RatingMatrixEntry> validations = splitStrategy.getValidationRatings();
//        List<RatingMatrixEntry> trainingRaings = splitStrategy.getTrainingMatrix();
//        Map<Integer, List<RatingMatrixEntry>> groupRatings = trainingRaings.parallelStream().collect(groupingBy(RatingMatrixEntry::getUserIndex));
//        System.out.println(groupRatings);

        PMFModel preUserModel = new PMFModel();
        PMFModel preItemModel = new PMFModel();
        final double a = 0.3;
        final double aPrime = 0.3;
        final double bPrime = 1;
        final int userNum = 100000;
        final int itemNum = 500000;
        final int featureCount = 100;
        final double maxOffsetShp = 0.01;
        final double maxOffsetRte = 0.1;
        final long seed = 0L;

        StopWatch timer = new StopWatch();
        Random random = new Random(seed);
        timer.start();
        preUserModel.initialize(a, aPrime, bPrime, userNum, featureCount, maxOffsetShp, maxOffsetRte, random);
        preItemModel.initialize(a, aPrime, bPrime, itemNum, featureCount, maxOffsetShp, maxOffsetRte, random);
        timer.stop();
        System.out.println("time for parallel is " + timer.getTime());

        RealMatrix gammaShp = MatrixUtils.createRealMatrix(userNum, featureCount);
        RealMatrix gammaRte = MatrixUtils.createRealMatrix(userNum, featureCount);
        RealVector kappaShp = MatrixUtils.createRealVector(new double[userNum]);
        RealVector kappaRte = MatrixUtils.createRealVector(new double[userNum]);
        RealMatrix lambdaShp = MatrixUtils.createRealMatrix(itemNum, featureCount);
        RealMatrix lambdaRte = MatrixUtils.createRealMatrix(itemNum, featureCount);
        RealVector tauShp = MatrixUtils.createRealVector(new double[itemNum]);
        RealVector tauRte = MatrixUtils.createRealVector(new double[itemNum]);


        final double kRte = aPrime + featureCount;
        final double tRte = aPrime + featureCount;


        timer.reset();
        timer.start();
        Random random1 = new Random(seed);
        for (int u = 0; u < userNum; u++ ) {
            for (int k = 0; k < featureCount; k++) {
                double valueShp = a + maxOffsetShp*random1.nextDouble();
                double valueRte = aPrime + maxOffsetRte*random1.nextDouble();
                gammaShp.setEntry(u, k, valueShp);
                gammaRte.setEntry(u, k, valueRte);
            }

            double kShp = aPrime + maxOffsetShp*random1.nextDouble();
            kappaRte.setEntry(u, kRte);
            kappaShp.setEntry(u, kShp);
        }

        for (int i = 0; i < itemNum; i++ ) {
            for (int k = 0; k < featureCount; k++) {
                double valueShp = a + maxOffsetShp*random1.nextDouble();
                double valueRte = aPrime + maxOffsetRte*random1.nextDouble();
                lambdaShp.setEntry(i, k, valueShp);
                lambdaRte.setEntry(i, k, valueRte);
            }
            double tShp = aPrime + maxOffsetShp*random1.nextDouble();
            tauRte.setEntry(i, tRte);
            tauShp.setEntry(i, tShp);
        }
        timer.stop();
        System.out.println("time for sequential is " + timer.getTime());

//        Int2ObjectMap userModel = preUserModel.getRows();
//        Iterator<Int2ObjectMap.Entry<PMFModel.ModelEntry>> iter = userModel.int2ObjectEntrySet().iterator();
//        while (iter.hasNext()) {
//            Int2ObjectMap.Entry<PMFModel.ModelEntry> entry = iter.next();
//            int user = entry.getIntKey();
//            PMFModel.ModelEntry modelEntry = entry.getValue();
//            int featureNum = modelEntry.getGammaOrLambdaShp().length;
//            for (int k = 0; k < featureNum; k++) {
//                assertThat(user, equalTo(modelEntry.getRowNumber()));
//                assertThat(gammaShp.getEntry(user, k), equalTo(modelEntry.getGammaOrLambdaShpEntry(k)));
//                assertThat(gammaRte.getEntry(user, k), equalTo(modelEntry.getGammaOrLambdaRteEntry(k)));
//                assertThat(kappaShp.getEntry(user), equalTo(modelEntry.getKappaOrTauShpEntry()));
//                assertThat(kappaRte.getEntry(user), equalTo(modelEntry.getKappaOrTauRteEntry()));
//
//            }
//        }
//
//        Int2ObjectMap itemModel = preItemModel.getRows();
//        Iterator<Int2ObjectMap.Entry<PMFModel.ModelEntry>> iterItem = itemModel.int2ObjectEntrySet().iterator();
//        while (iterItem.hasNext()) {
//            Int2ObjectMap.Entry<PMFModel.ModelEntry> entry = iterItem.next();
//            int item = entry.getIntKey();
//            PMFModel.ModelEntry modelEntry = entry.getValue();
//            int featureNum = modelEntry.getGammaOrLambdaShp().length;
//            for (int k = 0; k < featureNum; k++) {
//                assertThat(item, equalTo(modelEntry.getRowNumber()));
//                assertThat(gammaShp.getEntry(item, k), equalTo(modelEntry.getGammaOrLambdaShpEntry(k)));
//                assertThat(gammaRte.getEntry(item, k), equalTo(modelEntry.getGammaOrLambdaRteEntry(k)));
//                assertThat(kappaShp.getEntry(item), equalTo(modelEntry.getKappaOrTauShpEntry()));
//                assertThat(kappaRte.getEntry(item), equalTo(modelEntry.getKappaOrTauRteEntry()));
//
//            }
//        }


//
//        PFHyperParameters hyperParameters = new PFHyperParameters(0.3, 0.3, 1, 0.3, 0.3, 1, 5);
//        PMFModel model = groupRatings.values().parallelStream().map(e -> PMFModel.computeUserUpdate(e, preUserModel, preItemModel, hyperParameters)).collect(new PMFModelCollector());
//
//        System.out.println(groupRatings);
    }

}