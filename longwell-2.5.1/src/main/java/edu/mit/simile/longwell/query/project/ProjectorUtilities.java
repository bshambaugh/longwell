package edu.mit.simile.longwell.query.project;

public class ProjectorUtilities {

    static public class Pair {
        Object m_object;
        long m_value;
    }

    static public class DoublePair {
        Object m_object;
        double m_value;
    }

    final static public Pair[] mergeSort(Pair[] pairs, int start, int end) {
        int count = end - start;

        Pair[] newPairs = new Pair[count];
        if (count == 1) {
            newPairs = new Pair[1];
            newPairs[0] = pairs[start];
        } else if (count == 2) {
            newPairs = new Pair[2];

            if (pairs[start].m_value <= pairs[start + 1].m_value) {
                newPairs[0] = pairs[start];
                newPairs[1] = pairs[start + 1];
            } else {
                newPairs[0] = pairs[start + 1];
                newPairs[1] = pairs[start];
            }
        } else if (count > 2) {
            int mid = start + count / 2;

            Pair[] left = mergeSort(pairs, start, mid);
            Pair[] right = mergeSort(pairs, mid, end);

            newPairs = new Pair[count];

            int newIndex = 0;
            int leftIndex = 0;
            int rightIndex = 0;

            while (newIndex < count) {
                while (leftIndex < left.length
                        && (rightIndex >= right.length || left[leftIndex].m_value <= right[rightIndex].m_value)) {

                    newPairs[newIndex++] = left[leftIndex++];
                }
                while (rightIndex < right.length
                        && (leftIndex >= left.length || left[leftIndex].m_value >= right[rightIndex].m_value)) {

                    newPairs[newIndex++] = right[rightIndex++];
                }
            }
        }
        return newPairs;
    }

    final static int lookupIndex(Pair[] pairs, long equalOrGreater) {
        if (pairs.length == 0 || equalOrGreater > pairs[pairs.length - 1].m_value) {
            return -1;
        } else if (equalOrGreater <= pairs[0].m_value) {
            return 0;
        }

        int start = 0;
        int end = pairs.length;

        while (end - start > 1) {
            int mid = (start + end) / 2;
            long midValue = pairs[mid].m_value;

            if (midValue >= equalOrGreater) {
                end = mid;
            } else {
                start = mid;
            }
        }

        if (pairs[start].m_value >= equalOrGreater) {
            return start;
        }
        return start + 1;
    }

    final static public DoublePair[] mergeSort(DoublePair[] pairs, int start, int end) {
        int count = end - start;

        DoublePair[] newPairs = new DoublePair[count];
        if (count == 1) {
            newPairs = new DoublePair[1];
            newPairs[0] = pairs[start];
        } else if (count == 2) {
            newPairs = new DoublePair[2];

            if (pairs[start].m_value <= pairs[start + 1].m_value) {
                newPairs[0] = pairs[start];
                newPairs[1] = pairs[start + 1];
            } else {
                newPairs[0] = pairs[start + 1];
                newPairs[1] = pairs[start];
            }
        } else if (count > 2) {
            int mid = start + count / 2;

            DoublePair[] left = mergeSort(pairs, start, mid);
            DoublePair[] right = mergeSort(pairs, mid, end);

            newPairs = new DoublePair[count];

            int newIndex = 0;
            int leftIndex = 0;
            int rightIndex = 0;

            while (newIndex < count) {
                while (leftIndex < left.length
                        && (rightIndex >= right.length || left[leftIndex].m_value <= right[rightIndex].m_value)) {

                    newPairs[newIndex++] = left[leftIndex++];
                }
                while (rightIndex < right.length
                        && (leftIndex >= left.length || left[leftIndex].m_value >= right[rightIndex].m_value)) {

                    newPairs[newIndex++] = right[rightIndex++];
                }
            }
        }
        return newPairs;
    }

    final static int lookupIndex(DoublePair[] pairs, double equalOrGreater) {
        if (pairs.length == 0 || equalOrGreater > pairs[pairs.length - 1].m_value) {
            return -1;
        } else if (equalOrGreater <= pairs[0].m_value) {
            return 0;
        }

        int start = 0;
        int end = pairs.length;

        while (end - start > 1) {
            int mid = (start + end) / 2;
            double midValue = pairs[mid].m_value;

            if (midValue >= equalOrGreater) {
                end = mid;
            } else {
                start = mid;
            }
        }

        if (pairs[start].m_value >= equalOrGreater) {
            return start;
        }
        return start + 1;
    }
}
