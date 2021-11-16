/*
 * Copyright (C) 2021 Romain Guy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef PATHWAY_PATH_ITERATOR_H
#define PATHWAY_PATH_ITERATOR_H

#include "Path.h"

class PathIterator {
public:
    enum class VerbDirection {
        FORWARD, // API >=30
        BACKWARD // API < 30
    };

    PathIterator(
            Point* points,
            Verb* verbs,
            float* conicWeights,
            int count,
            VerbDirection direction
    ) noexcept
            : mPoints(points),
              mVerbs(verbs),
              mConicWeights(conicWeights),
              mCount(count),
              mDirection(direction) {
    }

    bool hasNext() const noexcept { return mCount > 0; }

    Verb peek() const noexcept {
        auto verbs = mDirection == VerbDirection::FORWARD ? mVerbs : mVerbs - 1;
        return mCount > 0 ? *verbs : Verb::Done;
    }

    Verb next(Point points[4]) noexcept;

private:
    Point* mPoints;
    Verb* mVerbs;
    float* mConicWeights;
    int mCount;
    VerbDirection mDirection;
};

#endif //PATHWAY_PATH_ITERATOR_H
