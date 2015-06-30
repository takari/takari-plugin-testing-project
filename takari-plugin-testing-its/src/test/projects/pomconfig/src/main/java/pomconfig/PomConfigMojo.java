package pomconfig;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "pomconfig")
public class PomConfigMojo extends AbstractMojo {

  @Parameter(defaultValue = "${project.build.directory}/output.txt")
  private File output;

  @Parameter(property = "text", defaultValue = "text")
  private String text;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    output.getParentFile().mkdirs();
    try (Writer w = new OutputStreamWriter(new FileOutputStream(output), "UTF-8")) {
      w.write("text=" + text);
    } catch (IOException e) {
      throw new MojoExecutionException("Could not create output file", e);
    }
  }

}
