/*
 * Copyright 2016 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.idea.blaze.java.run;

import com.google.idea.blaze.base.ideinfo.TargetIdeInfo;
import com.google.idea.blaze.base.ideinfo.TestIdeInfo;
import com.google.idea.blaze.base.model.primitives.Label;
import com.google.idea.blaze.base.run.TestTargetFinder;
import com.google.idea.blaze.base.run.TestTargetHeuristic;
import com.google.idea.blaze.base.run.targetfinder.TargetFinder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import java.io.File;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Utility methods for finding rules and Android facets. */
public final class RunUtil {

  private RunUtil() {}

  /**
   * @return The Blaze test rule containing the target test class. In the case of multiple
   *     containing rules, the first rule sorted alphabetically by label.
   */
  @Nullable
  public static TargetIdeInfo targetForTestClass(
      @NotNull Project project,
      @NotNull PsiClass testClass,
      @Nullable TestIdeInfo.TestSize testSize) {
    File testFile = getFileForClass(testClass);
    if (testFile == null) {
      return null;
    }
    Collection<TargetIdeInfo> targets =
        TestTargetFinder.getInstance(project).testTargetsForSourceFile(testFile);
    Label testLabel =
        TestTargetHeuristic.chooseTestTargetForSourceFile(testFile, targets, testSize);
    if (testLabel == null) {
      return null;
    }
    return TargetFinder.getInstance().targetForLabel(project, testLabel);
  }

  /**
   * Returns an instance of {@link java.io.File} related to the containing file of the given class.
   * It returns {@code null} if the given class is not contained in a file and only exists in
   * memory.
   */
  @Nullable
  public static File getFileForClass(@NotNull PsiClass aClass) {
    PsiFile containingFile = aClass.getContainingFile();
    if (containingFile == null) {
      return null;
    }

    VirtualFile virtualFile = containingFile.getVirtualFile();
    if (virtualFile == null) {
      return null;
    }

    return new File(virtualFile.getPath());
  }
}
