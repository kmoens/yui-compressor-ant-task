package com.simonbuckle.ant.tasks;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Mapper;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;

import com.yahoo.platform.yui.compressor.CssCompressor;
import com.yahoo.platform.yui.compressor.JavaScriptCompressor;


/**
 * @author Simon Buckle
 */
public class CompressTask extends Task {

	private List<FileSet> filesets = new ArrayList<FileSet>();
	private Mapper mapper;
	private int linebreak = -1;
	private boolean munge = true;
	private boolean preserveAllSemiColons = false;
	private boolean disableOptimizations = false;
	private boolean verbose = false;
	private String encoding = "UTF-8";
	private String todir;

	public void addFileset(final FileSet fileset) {
		filesets.add(fileset);
	}

	public void addMapper(final Mapper mapper) {
		this.mapper = mapper;
	}

	public void setDisableOptimizations(final boolean disableOptimizations) {
		this.disableOptimizations = disableOptimizations;
	}

	public void setLinebreak(final int linebreak) {
		this.linebreak = linebreak;
	}

	public void setMunge(final boolean munge) {
		this.munge = munge;
	}

	public void setPreserveAllSemiColons(final boolean preserveAllSemiColons) {
		this.preserveAllSemiColons = preserveAllSemiColons;
	}

	public void setToDir(final String todir) {
		this.todir = todir;
	}

	public void setVerbose(final boolean verbose) {
		this.verbose = verbose;
	}

	public void setEncoding(final String encoding) {
	    this.encoding = encoding;
	}

	private void validateRequired() throws BuildException {
		StringBuilder errorString = new StringBuilder();

		if (mapper == null) {
            errorString.append("Mapper property is required\n");
        }
		if (todir == null || "".equals(todir)) {
            errorString.append("Output directory is not specified\n");
        }

		if (errorString.length()>0) {
			throw new BuildException(errorString.toString());
		}
	}

	@Override
    public void execute() throws BuildException {
		validateRequired();

		Iterator<FileSet> iter = filesets.listIterator();
		while (iter.hasNext()) {
			FileSet fileset = iter.next();
			DirectoryScanner scanner = fileset.getDirectoryScanner(getProject());
			File dir = scanner.getBasedir();

			String[] files = scanner.getIncludedFiles();
			for (int i = 0; i < files.length; i++) {
				String fileName = files[i];
				String[] output = mapper.getImplementation().mapFileName(fileName);
				if (output != null) {
					try {
						if (fileName.endsWith("css")) {
							compressCss(new File(dir, fileName), new File(todir, output[0]));
						} else {
							compress(new File(dir, fileName), new File(todir, output[0]));
						}

					} catch (IOException io) {
					    io.printStackTrace();
						throw new BuildException("Failed to compress file: " + fileName, io);
					}
				}
			}
		}
	}

	private void compress(final File source, final File dest) throws IOException {
		Reader in = null;
		Writer out = null;
		try {
			in = new BufferedReader(new InputStreamReader(new FileInputStream(source), encoding));
			JavaScriptCompressor compressor = new JavaScriptCompressor(in, new ErrorReporter() {

				@Override
                public void warning(final String message, final String sourceName, final int line, final String lineSource, final int lineOffset) {
					log("Warning: " + message, Project.MSG_WARN);
				}

				@Override
                public void error(final String message, final String sourceName, final int line, final String lineSource, final int lineOffset) {
					log("Error: " + message, Project.MSG_ERR);
				}

				@Override
                public EvaluatorException runtimeError(final String message, final String sourceName, final int line, final String lineSource, final int lineOffset) {
					return new EvaluatorException(message);
				}

			});

			dest.getParentFile().mkdirs();

			out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(dest), encoding));
			log("Compressing: " + source);


			compressor.compress(out,
					linebreak,
					munge,
					verbose,
					preserveAllSemiColons,
					disableOptimizations);
		} finally {
			if (in != null) {
                in.close();
            }
			if (out != null) {
                out.close();
            }
		}
	}

	private void compressCss(final File source, final File dest) throws IOException {
		Reader in = null;
		Writer out = null;
		try {
			in = new BufferedReader(new InputStreamReader(new FileInputStream(source), encoding));
			CssCompressor compressor = new CssCompressor(in);

			log("Compressing: " + source);

			dest.getParentFile().mkdirs();

			out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(dest), encoding));
			compressor.compress(out, linebreak);

		} finally {
			if (in != null) {
                in.close();
            }
			if (out != null) {
                out.close();
            }
		}
	}
}
