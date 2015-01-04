package basic;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "basic")
public class BasicMojo extends AbstractMojo {

  @Parameter(defaultValue = "${project.build.directory}/output.txt")
  private File output;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    output.getParentFile().mkdirs();
    try {
      new FileOutputStream(output).close();
    } catch (IOException e) {
      throw new MojoExecutionException("Could not create output file", e);
    }
  }

}
