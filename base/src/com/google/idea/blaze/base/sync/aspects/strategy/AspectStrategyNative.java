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
package com.google.idea.blaze.base.sync.aspects.strategy;

import com.google.idea.blaze.base.command.BlazeCommand;
import com.google.repackaged.devtools.intellij.ideinfo.IntellijIdeInfo;
import java.io.IOException;
import java.io.InputStream;

/** Aspect strategy for native. */
public class AspectStrategyNative implements AspectStrategy {

  @Override
  public String getName() {
    return "NativeAspect";
  }

  @Override
  public void modifyIdeInfoCommand(BlazeCommand.Builder blazeCommandBuilder) {
    blazeCommandBuilder
        .addBlazeFlags("--aspects=AndroidStudioInfoAspect")
        .addBlazeFlags("--output_groups=ide-info");
  }

  @Override
  public void modifyIdeResolveCommand(BlazeCommand.Builder blazeCommandBuilder) {
    blazeCommandBuilder
        .addBlazeFlags("--aspects=AndroidStudioInfoAspect")
        .addBlazeFlags("--output_groups=ide-resolve");
  }

  @Override
  public void modifyIdeCompileCommand(BlazeCommand.Builder blazeCommandBuilder) {
    blazeCommandBuilder
        .addBlazeFlags("--aspects=AndroidStudioInfoAspect")
        .addBlazeFlags("--output_groups=ide-compile");
  }

  @Override
  public String getAspectOutputFileExtension() {
    return ".aswb-build";
  }

  @Override
  public IntellijIdeInfo.TargetIdeInfo readAspectFile(InputStream inputStream) throws IOException {
    return IntellijIdeInfo.TargetIdeInfo.parseFrom(inputStream);
  }
}
