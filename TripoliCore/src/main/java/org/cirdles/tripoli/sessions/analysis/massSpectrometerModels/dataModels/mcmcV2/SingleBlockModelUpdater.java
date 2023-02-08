/*
 * Copyright 2022 James Bowring, Noah McLean, Scott Burdick, and CIRDLES.org.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cirdles.tripoli.sessions.analysis.massSpectrometerModels.dataModels.mcmcV2;

import com.google.common.primitives.Ints;
import jama.Matrix;
import org.apache.commons.math3.random.RandomDataGenerator;
import org.apache.commons.math3.stat.correlation.Covariance;
import org.ojalgo.matrix.store.PhysicalStore;
import org.ojalgo.matrix.store.Primitive64Store;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author James F. Bowring
 */
public enum SingleBlockModelUpdater {
    ;


    /*
    Scott Burdick via email 5 Feb 2023
    Here are the operation permutation generator and two functions that needed to be altered to accommodate it.  PreorderOpsMS takes in
    the number of parameters and permutes them as many times as necessary.
    It can be placed anywhere after the initial model is defined but before the main loop:oper_order = PreorderOpsMS(x,maxcnt*datsav);

    The other two functions replace the existing versions in the main loop:

    oper = RandomOperMS_Preorder(x,oper_order(m));

    [x2,delx] = UpdateMSv2_Preorder(oper_order(m),x,psig,prior,ensemble,xcov,delx_adapt,adaptflag,allflag);

    Unfortunately it seemed easier to keep in the “oper” string variable since it’s used in so many other places,
    but now it’s defined according to the precomputed order.
     */

    /*
    function oper_order = PreorderOpsMS(x,cnt)
    oper_order = PreorderOpsMS(x,maxcnt*datsav);
        % Inputs:
        % x - struct containing model variables
        % cnt - Number of random walk iterations to perform before adaptive MC
        % Outputs:
        % oper_order

        % Find number of variables for each parameter type from model struct
        Niso = length(x.lograt);
        Nblock = length(x.I);
        for ii=1:Nblock;
            Ncycle(ii) = length(x.I{ii});
        end
        Nfar = length(x.BL);
        Ndf = 1;
        Nsig = length(x.sig);

        % Total number of variables
        N = Niso+sum(Ncycle)+Nfar+Ndf+Nsig;

        % How many permutations needed?
        Npermutes = ceil(cnt/N);

        oper_order= zeros(N*Npermutes,1);

        for ii = 1:Npermutes
            oper_order((1+(ii-1)*N):ii*N,1) = randperm(N)';
        end
     */

    static int countOfLogRatios;
    static int countOfCycles;
    static int countOfFaradays;
    static int countOfPhotoMultipliers;
    static int countTotalVariables;
    static int countOfTotalModelParameters;
    static int countOfSignalNoiseSigma;

    public static List<String> operations = new ArrayList<>();

    static {
        operations.add("changer");
        operations.add("changeI");
        operations.add("changedfg");
        operations.add("changebl");
        operations.add("noise");
    }

    public static int[] preOrderOpsMS(SingleBlockModelRecord singleBlockInitialModelRecord, int countOfIterations) {
        countOfLogRatios = singleBlockInitialModelRecord.logRatios().length;
        countOfCycles = singleBlockInitialModelRecord.I0().length;
        countOfFaradays = singleBlockInitialModelRecord.faradayCount();
        countOfPhotoMultipliers = 1;
        countOfSignalNoiseSigma = singleBlockInitialModelRecord.signalNoiseSigma().length;

        countTotalVariables = countOfLogRatios + countOfCycles + countOfFaradays + countOfPhotoMultipliers + countOfSignalNoiseSigma;
        countOfTotalModelParameters = countOfLogRatios + countOfCycles + countOfFaradays + countOfPhotoMultipliers;
        int countOfPermutations = (int) StrictMath.ceil(countOfIterations / countTotalVariables) + 1;

        int[] operationOrder = new int[countOfPermutations * countTotalVariables];

        Integer[] permuteArray = new Integer[countTotalVariables];
        for (int i = 0; i < countTotalVariables; i++) {
            permuteArray[i] = i;
        }
        List<Integer> permuteList = Arrays.asList(permuteArray);

        for (int permIndex = 0; permIndex < countOfPermutations; permIndex++) {
            Collections.shuffle(permuteList);
            System.arraycopy(Ints.toArray(permuteList), 0, operationOrder, permIndex * countTotalVariables, countTotalVariables);
        }
        return operationOrder;
    }

    /*
    function oper = RandomOperMS_Preorder(x,oper_order)

    % Randomly generate next model operation, with or without hierarchical step

    % Find number of variables for each parameter type from model struct
    Niso = length(x.lograt);
    Nblock = length(x.I);
    for ii=1:Nblock;
        Ncycle(ii) = length(x.I{ii});
    end
    Nfar = length(x.BL);
    Ndf = 1;
    Nsig = length(x.sig);

    if oper_order <= Niso;
        oper = 'changer';
    elseif oper_order <= Niso + sum(Ncycle);
        oper = 'changeI';
    elseif oper_order <= Niso + sum(Ncycle) + Nfar
        oper = 'changebl';
    elseif oper_order <= Niso + sum(Ncycle) + Nfar + randi(Ndf)
        oper = 'changedfg';
    else
        oper = 'noise';
    end
     */

    public static String randomOperMS_Preorder(int oper_order) {
        RandomDataGenerator randomDataGenerator = new RandomDataGenerator();
        randomDataGenerator.reSeedSecure();

        String oper = "noise";
        if (oper_order <= countOfLogRatios)
            oper = "changer";
        else if (oper_order <= countOfLogRatios + countOfCycles)
            oper = "changeI";
        else if (oper_order <= countOfLogRatios + countOfCycles + countOfFaradays)
            oper = "changebl";
        else if (oper_order <= countOfLogRatios + countOfCycles + countOfFaradays + randomDataGenerator.nextInt(1, countOfPhotoMultipliers))
            oper = "changedfg";

        return oper;
    }


    public static String randomOperation(boolean hierFlag) {
        Object[][] notHier = {{40, 60, 80, 100}, {"changeI", "changer", "changebl", "changedfg"}};
        Object[][] hier = {{60, 80, 90, 100, 120}, {"changeI", "changer", "changebl", "changedfg", "noise"}};
//        Object[][] hier = new Object[][]{{400, 440, 520, 540, 600}, {"changeI", "changer", "changebl", "changedfg", "noise"}};

        RandomDataGenerator randomDataGenerator = new RandomDataGenerator();
        randomDataGenerator.reSeedSecure();
        int choice = hierFlag ? randomDataGenerator.nextInt(0, 120) : randomDataGenerator.nextInt(0, 100);
        String retVal = "changeI";
        if (hierFlag) {
            for (int i = 0; i < hier[0].length; i++) {
                if (choice < (int) hier[0][i]) {
                    retVal = (String) hier[1][i];
                    break;
                }
            }
        } else {
            for (int i = 0; i < notHier[0].length; i++) {
                if (choice < (int) notHier[0][i]) {
                    retVal = (String) notHier[1][i];
                    break;
                }
            }
        }

        return retVal;
    }

    /*
    function  [x2,delx,xcov] = UpdateMSv2_Preorder(oper,x,psig,prior,ensemble,xcov,delx_adapt,adaptflag,allflag)
        %%
        cnt = length(ensemble);
        ps0diag =  [psig.lograt*ones(Niso,1);          psig.I*ones(sum(Ncycle),1);     psig.BL*ones(Nfar,1);     psig.DFgain*ones(Ndf,1)];
        priormin = [prior.lograt(1)*ones(Niso-1,1); 0; prior.I(1)*ones(sum(Ncycle),1); prior.BL(1)*ones(Nfar,1); prior.DFgain(1)*ones(Ndf,1)];
        priormax = [prior.lograt(2)*ones(Niso-1,1); 0; prior.I(2)*ones(sum(Ncycle),1); prior.BL(2)*ones(Nfar,1); prior.DFgain(2)*ones(Ndf,1)];
        %xx0 = [x.lograt; x.I{1}; x.I{2}; x.BL; x.DFgain];

        xx0 = x.lograt;
        xind = ones(Niso,1);

        for ii=1:Nblock
            xx0 = [xx0; x.I{ii}];
            xind = [xind; 1+ii*ones(Ncycle(ii),1)];
        end

        xx0 = [xx0; x.BL];
        xind = [xind; (2+Nblock)*ones(Nfar,1)];

        xx0 = [xx0; x.DFgain];
        xind = [xind; (3+Nblock)*ones(Ndf,1)];

        %if strcmp(oper(1:3),'cha')
        if oper<=N % If operation is for model parameter, not noise parameter
            if ~allflag
                if adaptflag
                    delx = sqrt(xcov(oper,oper))*randn(1); %mvnrnd(zeros(1),xcov(nind,nind));
                else
                    delx = ps0diag(oper)*randn(1);
                end
                xx =  xx0;
                xx(oper) = xx(oper) + delx;

                inprior = xx<=priormax & xx>=priormin;

                xx(~inprior) = xx0(~inprior);
            else
                %VARY ALL AT A TIME

                %%delx(:,1) = mvnrnd(zeros(size(xx0)),xcov);
                %delx(:,1) = mvnrnd(zeros(size(xx0)),2.38^2*xcov/length(xind));
                delx = delx_adapt;
                xx =  xx0 + delx;
                inprior = xx<=priormax & xx>=priormin;
                xx(~inprior) = xx0(~inprior);
            end
             x2.lograt = xx(xind==1);
            for ii=1:Nblock
                x2.I{ii} = xx(xind==(1+ii));
            end
            x2.BL = xx(xind==(2+Nblock));
            x2.DFgain = xx(xind==(3+Nblock));

            x2.sig = x.sig;

        elseif oper>N   %CHANGE NOISE

            %oper = randi(length(x.sig));
            %oper = randi(length(x.BL)); %Just for the faradays

            % Find preordered random noise variable
            nind = oper - N;
            x2=x;

            delx=psig.sig*randn(1);

            if x2.sig(nind) + delx >= prior.sig(1) && x2.sig(nind) + delx <= prior.sig(2)
                x2.sig(nind) = x2.sig(nind)+delx;
            else
                delx=0;
            end
        else
            disp('Thats not a thing')
        end
     */

    public static SingleBlockModelRecord updateMSv2Preorder(
            int operationIndex,
            SingleBlockModelRecord singleBlockInitialModelRecord_initial,
            ProposedModelParameters.ProposalSigmasRecord psigRecord,
            ProposedModelParameters.ProposalRangesRecord priorRecord,
            double[][] xDataCovariance,
            double[] delx_adapt,
            boolean adaptiveFlag,
            boolean allFlag) {

        //TODO: these should only be done once ?
        double[] ps0DiagArray = new double[countOfTotalModelParameters];
        double[] priorMinArray = new double[countOfTotalModelParameters];
        double[] priorMaxArray = new double[countOfTotalModelParameters];

        for (int logRatioIndex = 0; logRatioIndex < countOfLogRatios; logRatioIndex++) {
            ps0DiagArray[logRatioIndex] = psigRecord.psigLogRatio();
            priorMinArray[logRatioIndex] = priorRecord.priorLogRatio()[0][0];
            priorMaxArray[logRatioIndex] = priorRecord.priorLogRatio()[0][1];
        }

        for (int cycleIndex = 0; cycleIndex < countOfCycles; cycleIndex++) {
            ps0DiagArray[cycleIndex + countOfLogRatios] = psigRecord.psigIntensityPercent();
            priorMinArray[cycleIndex + countOfLogRatios] = priorRecord.priorIntensity()[0][0];
            priorMaxArray[cycleIndex + countOfLogRatios] = priorRecord.priorIntensity()[0][1];
        }

        for (int faradayIndex = 0; faradayIndex < countOfFaradays; faradayIndex++) {
            ps0DiagArray[faradayIndex + countOfLogRatios + countOfCycles] = psigRecord.psigBaselineFaraday();
            priorMinArray[faradayIndex + countOfLogRatios + countOfCycles] = priorRecord.priorBaselineFaraday()[0][0];
            priorMaxArray[faradayIndex + countOfLogRatios + countOfCycles] = priorRecord.priorBaselineFaraday()[0][1];
        }
        ps0DiagArray[countOfTotalModelParameters - 1] = psigRecord.psigDFgain();
        priorMinArray[countOfTotalModelParameters - 1] = priorRecord.priorDFgain()[0][0];
        priorMaxArray[countOfTotalModelParameters - 1] = priorRecord.priorDFgain()[0][1];

        double[] parametersModel_xx0 = new double[countOfTotalModelParameters];
        // parametersModelTypeFlags contains indices where 1 = logratio, 2 = cycles from I0, 3 = baseline means, 4 = faradayGain
        double[] parametersModelTypeFlags = new double[countOfTotalModelParameters];
        System.arraycopy(singleBlockInitialModelRecord_initial.logRatios(), 0, parametersModel_xx0, 0, countOfLogRatios);
        Arrays.fill(parametersModelTypeFlags, 1.0);

        System.arraycopy(singleBlockInitialModelRecord_initial.I0(), 0, parametersModel_xx0, countOfLogRatios, countOfCycles);
        double[] temp = new double[countOfCycles];
        Arrays.fill(temp, 2.0);
        System.arraycopy(temp, 0, parametersModelTypeFlags, countOfLogRatios, countOfCycles);

        System.arraycopy(singleBlockInitialModelRecord_initial.baselineMeansArray(), 0, parametersModel_xx0, countOfLogRatios + countOfCycles, countOfFaradays);
        temp = new double[countOfFaradays];
        Arrays.fill(temp, 3.0);
        System.arraycopy(temp, 0, parametersModelTypeFlags, countOfLogRatios + countOfCycles, countOfFaradays);

        parametersModel_xx0[countOfTotalModelParameters - 1] = singleBlockInitialModelRecord_initial.detectorFaradayGain();
        parametersModelTypeFlags[countOfTotalModelParameters - 1] = 4;

        SingleBlockModelRecord singleBlockInitialModelRecord_initial2 = null;
        RandomDataGenerator randomDataGenerator = new RandomDataGenerator();
        randomDataGenerator.reSeedSecure();

        double randomSigma = 1.0;
        double[] signalNoiseSigmaUpdated = singleBlockInitialModelRecord_initial.signalNoiseSigma().clone();
        double[] parametersModel_updated = parametersModel_xx0.clone();

        double deltaX;
        if (operationIndex < countOfTotalModelParameters) {
            if (!allFlag) {
                if (adaptiveFlag) {
                    deltaX = StrictMath.sqrt(xDataCovariance[operationIndex][operationIndex]) * randomDataGenerator.nextGaussian(0.0, randomSigma);
                } else {
                    deltaX = ps0DiagArray[operationIndex] * randomDataGenerator.nextGaussian(0.0, randomSigma);
                }

                double changed = parametersModel_updated[operationIndex] + deltaX;
                if ((changed <= priorMaxArray[operationIndex] && (changed >= priorMinArray[operationIndex]))) {
                    parametersModel_updated[operationIndex] = changed;
                }
            } else {
                //        %VARY ALL AT A TIME
                //                    delx = delx_adapt;
                //            xx = xx0 + delx;
                //            inprior = xx <= priormax & xx >= priormin;
                //            xx(~inprior) = xx0(~inprior);
            }

            List<Double> updatedLogRatioList = new ArrayList<>();
            List<Double> updatedBlockintensitiesListII = new ArrayList<>();
            List<Double> updatedBaselineMeansList = new ArrayList<>();
            double updatedDFGain = 0.0;

            for (int row = 0; row < parametersModel_updated.length; row++) {
                if (1 == parametersModelTypeFlags[row]) {
                    updatedLogRatioList.add(parametersModel_updated[row]);
                }
                if (2 == parametersModelTypeFlags[row]) {
                    updatedBlockintensitiesListII.add(parametersModel_updated[row]);
                }
                if (3 == parametersModelTypeFlags[row]) {
                    updatedBaselineMeansList.add(parametersModel_updated[row]);
                }
                if (4 == parametersModelTypeFlags[row]) {
                    updatedDFGain = parametersModel_updated[row];
                }
            }
            double[] updatedLogRatio = updatedLogRatioList.stream().mapToDouble(d -> d).toArray();
            double[] updatedBaselineMeans = updatedBaselineMeansList.stream().mapToDouble(d -> d).toArray();
            double[] updatedBlockIntensities_I0 = updatedBlockintensitiesListII.stream().mapToDouble(d -> d).toArray();

            singleBlockInitialModelRecord_initial2 = new SingleBlockModelRecord(
                    updatedBaselineMeans,
                    singleBlockInitialModelRecord_initial.baselineStandardDeviationsArray().clone(),
                    updatedDFGain,
                    singleBlockInitialModelRecord_initial.mapDetectorOrdinalToFaradayIndex(),
                    updatedLogRatio,
                    singleBlockInitialModelRecord_initial.signalNoiseSigma().clone(),
                    singleBlockInitialModelRecord_initial.dataArray().clone(),
                    singleBlockInitialModelRecord_initial.dataWithNoBaselineArray().clone(),
                    singleBlockInitialModelRecord_initial.dataSignalNoiseArray().clone(),
                    updatedBlockIntensities_I0,
                    singleBlockInitialModelRecord_initial.intensities(),
                    singleBlockInitialModelRecord_initial.faradayCount(),
                    singleBlockInitialModelRecord_initial.isotopeCount()
            );

        } else {
            // noise case
            /*
            % Find preordered random noise variable
            nind = oper - N;
            x2=x;
            delx=psig.sig*randn(1);

            if x2.sig(nind) + delx >= prior.sig(1) && x2.sig(nind) + delx <= prior.sig(2)
                x2.sig(nind) = x2.sig(nind)+delx;
            else
                delx=0;
            end
             */
            int nInd = operationIndex - countOfTotalModelParameters;
            deltaX = psigRecord.psigSignalNoiseFaraday() * randomDataGenerator.nextGaussian(0.0, randomSigma);
            double testDelta = signalNoiseSigmaUpdated[nInd] + deltaX;
            if ((testDelta >= priorRecord.priorSignalNoiseFaraday()[0][0])
                    &&
                    (testDelta <= priorRecord.priorSignalNoiseFaraday()[0][1])) {
                signalNoiseSigmaUpdated[nInd] = testDelta;
            }
            singleBlockInitialModelRecord_initial2 = new SingleBlockModelRecord(
                    singleBlockInitialModelRecord_initial.baselineMeansArray(),
                    singleBlockInitialModelRecord_initial.baselineStandardDeviationsArray(),
                    singleBlockInitialModelRecord_initial.detectorFaradayGain(),
                    singleBlockInitialModelRecord_initial.mapDetectorOrdinalToFaradayIndex(),
                    singleBlockInitialModelRecord_initial.logRatios(),
                    signalNoiseSigmaUpdated,
                    singleBlockInitialModelRecord_initial.dataArray(),
                    singleBlockInitialModelRecord_initial.dataWithNoBaselineArray(),
                    singleBlockInitialModelRecord_initial.dataSignalNoiseArray(),
                    singleBlockInitialModelRecord_initial.I0(),
                    singleBlockInitialModelRecord_initial.intensities(),
                    singleBlockInitialModelRecord_initial.faradayCount(),
                    singleBlockInitialModelRecord_initial.isotopeCount());
        }
        return singleBlockInitialModelRecord_initial2;
    }

    static UpdatedCovariancesRecord updateMeanCovMS(
            SingleBlockModelRecord singleBlockModelRecord,
            double[][] dataModelCov,
            double[] dataModelMean,
            List<EnsemblesStoreV2.EnsembleRecord> ensembleRecordsList,
            int countOfNewModels,
            boolean iterFlag) {
        // [xmean,xcov] = UpdateMeanCovMS(x,xcov,xmean,ensemble,cnt-covstart,0);
        // function [xmean,xcov] = UpdateMeanCovMS(x,xcov,xmean,ensemble,m,iterflag)
        /*
            Niso = length(x.lograt);
            Nblock = length(x.I);
            for ii=1:Nblock;
                Ncycle(ii) = length(x.I{ii});
            end
            Nfar = length(x.BL);
            Ndf = 1;
            Nmod = Niso + sum(Ncycle) + Nfar + Ndf;

            if iterflag
                xx = x.lograt;
                for ii=1:Nblock
                    xx = [xx; x.I{ii}];
                end
                xx = [xx; x.BL(1:Nfar)];
                xx = [xx; x.DFgain];
                xmean = (xmean*(m-1) + xx)/m;
                xctmp = (xx-xmean)*(xx-xmean)';
                xctmp = (xctmp+xctmp')/2;
                xcov = (xcov*(m-1) + (m-1)/m*xctmp)/m;
            end

            if ~iterflag
                cnt = length(ensemble);
                enso = [ensemble.lograt];
                for ii = 1:Nblock
                    for n = 1:cnt;
                        ens_I{ii}(:,n) =[ensemble(n).I{ii}];
                    end
                    enso = [enso; ens_I{ii}];
                end
                enso = [enso; [ensemble.BL]];
                enso = [enso; [ensemble.DFgain]];
                %xcov = cov(enso(:,ceil(end/2):end)');
                xmean = mean(enso(:,m:end)');
                xcov = cov(enso(:,m:end)');
            end
         */

        PhysicalStore.Factory<Double, Primitive64Store> storeFactory = Primitive64Store.FACTORY;
        double[] dataMean;
        double[][] dataCov;
        Covariance cov2 = new Covariance();
        if (iterFlag) {
            // todo: currently iterFlag is always false
            dataMean = null;
            dataCov = null;

                /*
                xx = x.lograt;
                for ii=1:Nblock
                    xx = [xx; x.I{ii}];
                end
                xx = [xx; x.BL(1:Nfar)];
                xx = [xx; x.DFgain];
                xmean = (xmean*(m-1) + xx)/m;
                xctmp = (xx-xmean)*(xx-xmean)';
                xctmp = (xctmp+xctmp')/2;
                xcov = (xcov*(m-1) + (m-1)/m*xctmp)/m;
             */


        } else {
            /*
                cnt = length(ensemble);
                enso = [ensemble.lograt];
                for ii = 1:Nblock
                    for n = 1:cnt;
                        ens_I{ii}(:,n) =[ensemble(n).I{ii}];
                    end
                    enso = [enso; ens_I{ii}];
                end
                enso = [enso; [ensemble.BL]];
                enso = [enso; [ensemble.DFgain]];

                %xcov = cov(enso(:,ceil(end/2):end)');
                xmean = mean(enso(:,m:end)');
                xcov = cov(enso(:,m:end)');
             */
            int modelCount = ensembleRecordsList.size() - countOfNewModels + 1;
            PhysicalStore<Double> totalsByRow = storeFactory.make(countOfTotalModelParameters, 1);
            PhysicalStore<Double> enso = storeFactory.make(countOfTotalModelParameters, modelCount);

            for (int modelIndex = 0; modelIndex < modelCount; modelIndex++) {
                EnsemblesStoreV2.EnsembleRecord ensembleRecord = ensembleRecordsList.get(modelIndex + countOfNewModels - 1);
                int row = 0;
                for (int logRatioIndex = 0; logRatioIndex < countOfLogRatios; logRatioIndex++) {
                    enso.set(row, modelIndex, ensembleRecord.logRatios()[logRatioIndex]);
                    totalsByRow.set(row, 0, totalsByRow.get(row, 0) + ensembleRecord.logRatios()[logRatioIndex]);
                    row++;
                }

                for (int intensityIndex = 0; intensityIndex < singleBlockModelRecord.I0().length; intensityIndex++) {
                    enso.set(row, modelIndex, ensembleRecord.intensities()[intensityIndex]);
                    totalsByRow.set(row, 0, totalsByRow.get(row, 0) + ensembleRecord.intensities()[intensityIndex]);
                    row++;
                }

                for (int baseLineIndex = 0; baseLineIndex < countOfFaradays; baseLineIndex++) {
                    enso.set(row, modelIndex, ensembleRecord.baseLine()[baseLineIndex]);
                    totalsByRow.set(row, 0, totalsByRow.get(row, 0) + ensembleRecord.baseLine()[baseLineIndex]);
                    row++;
                }

                enso.set(row, modelIndex, ensembleRecordsList.get(modelIndex + countOfNewModels - 1).dfGain());
                totalsByRow.set(row, 0, totalsByRow.get(row, 0) + ensembleRecord.dfGain());
            }

            for (int i = 0; i < totalsByRow.getRowDim(); i++) {
                totalsByRow.set(i, 0, totalsByRow.get(i, 0) / modelCount);
            }

            dataMean = totalsByRow.transpose().toRawCopy1D();
            cov2 = new Covariance(enso.transpose().toRawCopy2D());
        }
        return new UpdatedCovariancesRecord(cov2.getCovarianceMatrix().getData(), dataMean);
    }

    public record UpdatedCovariancesRecord(
            double[][] dataCov,
            double[] dataMean
    ) {

    }
}