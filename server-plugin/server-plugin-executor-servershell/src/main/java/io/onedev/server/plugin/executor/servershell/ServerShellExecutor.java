package io.onedev.server.plugin.executor.servershell;

import static io.onedev.agent.ShellExecutorUtils.testCommands;
import static io.onedev.k8shelper.KubernetesHelper.cloneRepository;
import static io.onedev.k8shelper.KubernetesHelper.installGitCert;
import static io.onedev.k8shelper.KubernetesHelper.replacePlaceholders;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.Size;

import org.apache.commons.lang.SystemUtils;

import io.onedev.agent.ExecutorUtils;
import io.onedev.agent.job.FailedException;
import io.onedev.commons.bootstrap.Bootstrap;
import io.onedev.commons.loader.AppLoader;
import io.onedev.commons.utils.ExplicitException;
import io.onedev.commons.utils.FileUtils;
import io.onedev.commons.utils.TaskLogger;
import io.onedev.commons.utils.command.Commandline;
import io.onedev.commons.utils.command.ExecutionResult;
import io.onedev.k8shelper.BuildImageFacade;
import io.onedev.k8shelper.CacheAllocationRequest;
import io.onedev.k8shelper.CacheInstance;
import io.onedev.k8shelper.CheckoutFacade;
import io.onedev.k8shelper.CloneInfo;
import io.onedev.k8shelper.CommandFacade;
import io.onedev.k8shelper.CompositeFacade;
import io.onedev.k8shelper.JobCache;
import io.onedev.k8shelper.LeafFacade;
import io.onedev.k8shelper.LeafHandler;
import io.onedev.k8shelper.OsExecution;
import io.onedev.k8shelper.OsInfo;
import io.onedev.k8shelper.RunContainerFacade;
import io.onedev.k8shelper.ServerSideFacade;
import io.onedev.server.OneDev;
import io.onedev.server.buildspec.job.JobContext;
import io.onedev.server.buildspec.job.JobManager;
import io.onedev.server.git.config.GitConfig;
import io.onedev.server.job.resource.ResourceManager;
import io.onedev.server.model.support.administration.jobexecutor.JobExecutor;
import io.onedev.server.plugin.executor.servershell.ServerShellExecutor.TestData;
import io.onedev.server.util.validation.annotation.Code;
import io.onedev.server.web.editable.annotation.Editable;
import io.onedev.server.web.editable.annotation.Horizontal;
import io.onedev.server.web.editable.annotation.OmitName;
import io.onedev.server.web.util.Testable;

@Editable(order=ServerShellExecutor.ORDER, name="Server Shell Executor", description="This executor runs build jobs with OneDev server's shell facility")
@Horizontal
public class ServerShellExecutor extends JobExecutor implements Testable<TestData> {

	private static final long serialVersionUID = 1L;
	
	static final int ORDER = 400;
	
	private static final Object cacheHomeCreationLock = new Object();
	
	private File getCacheHome() {
		File file = new File(Bootstrap.getSiteDir(), "cache");
		if (!file.exists()) synchronized (cacheHomeCreationLock) {
			FileUtils.createDir(file);
		}
		return file;
	}
	
	@Override
	public void execute(String jobToken, JobContext jobContext) {
		TaskLogger jobLogger = jobContext.getLogger();
		OneDev.getInstance(ResourceManager.class).run(new Runnable() {

			@Override
			public void run() {
				File buildDir = FileUtils.createTempDir("onedev-build");
				File workspaceDir = new File(buildDir, "workspace");
				try {
					jobLogger.log(String.format("Executing job with executor '%s'...", getName()));
					jobContext.notifyJobRunning(null);
					
					if (!jobContext.getServices().isEmpty()) {
						throw new ExplicitException("This job requires services, which can only be supported "
								+ "by docker aware executors");
					}
					
					JobManager jobManager = OneDev.getInstance(JobManager.class);		
					File cacheHomeDir = getCacheHome();
					
					jobLogger.log("Setting up job cache...") ;
					JobCache cache = new JobCache(cacheHomeDir) {

						@Override
						protected Map<CacheInstance, String> allocate(CacheAllocationRequest request) {
							return jobManager.allocateJobCaches(jobToken, request);							
						}

						@Override
						protected void clean(File cacheDir) {
							FileUtils.cleanDir(cacheDir);							
						}
						
					};
					cache.init(true);
					FileUtils.createDir(workspaceDir);
					
					cache.installSymbolinks(workspaceDir);
					
					jobLogger.log("Copying job dependencies...");
					jobContext.copyDependencies(workspaceDir);
					
					File userDir = new File(buildDir, "user");
					FileUtils.createDir(userDir);
					
					jobContext.reportJobWorkspace(workspaceDir.getAbsolutePath());
					
					CompositeFacade entryFacade = new CompositeFacade(jobContext.getActions());
					
					OsInfo osInfo = OneDev.getInstance(OsInfo.class);
					boolean successful = entryFacade.execute(new LeafHandler() {

						@Override
						public boolean execute(LeafFacade facade, List<Integer> position) {
							String stepNames = entryFacade.getNamesAsString(position);
							jobLogger.notice("Running step \"" + stepNames + "\"...");
							
							if (facade instanceof CommandFacade) {
								CommandFacade commandFacade = (CommandFacade) facade;
								OsExecution execution = commandFacade.getExecution(osInfo);
								if (execution.getImage() != null) {
									throw new ExplicitException("This step can only be executed by server docker executor, "
											+ "remote docker executor, or kubernetes executor");
								}
								
								File jobScriptFile = new File(buildDir, "job-commands" + commandFacade.getScriptExtension());
								try {
									FileUtils.writeLines(
											jobScriptFile, 
											new ArrayList<>(replacePlaceholders(execution.getCommands(), buildDir)), 
											commandFacade.getEndOfLine());
								} catch (IOException e) {
									throw new RuntimeException(e);
								}
								
								Commandline interpreter = commandFacade.getInterpreter();
								Map<String, String> environments = new HashMap<>();
								environments.put("GIT_HOME", userDir.getAbsolutePath());
								interpreter.workingDir(workspaceDir).environments(environments);
								interpreter.addArgs(jobScriptFile.getAbsolutePath());
								
								ExecutionResult result = interpreter.execute(ExecutorUtils.newInfoLogger(jobLogger), ExecutorUtils.newWarningLogger(jobLogger));
								if (result.getReturnCode() != 0) {
									jobLogger.error("Step \"" + stepNames + "\" is failed: Command exited with code " + result.getReturnCode());
									return false;
								}
							} else if (facade instanceof RunContainerFacade || facade instanceof BuildImageFacade) {
								throw new ExplicitException("This step can only be executed by server docker executor, "
										+ "remote docker executor, or kubernetes executor");
							} else if (facade instanceof CheckoutFacade) {
								try {
									CheckoutFacade checkoutFacade = (CheckoutFacade) facade;
									jobLogger.log("Checking out code...");
									Commandline git = new Commandline(AppLoader.getInstance(GitConfig.class).getExecutable());	
									
									checkoutFacade.setupWorkingDir(git, workspaceDir);
									
									Map<String, String> environments = new HashMap<>();
									environments.put("HOME", userDir.getAbsolutePath());
									git.environments(environments);

									CloneInfo cloneInfo = checkoutFacade.getCloneInfo();
									
									cloneInfo.writeAuthData(userDir, git, ExecutorUtils.newInfoLogger(jobLogger), ExecutorUtils.newWarningLogger(jobLogger));
									
									List<String> trustCertContent = getTrustCertContent();
									if (!trustCertContent.isEmpty()) {
										installGitCert(new File(userDir, "trust-cert.pem"), trustCertContent, 
												git, ExecutorUtils.newInfoLogger(jobLogger), ExecutorUtils.newWarningLogger(jobLogger));
									}

									int cloneDepth = checkoutFacade.getCloneDepth();
									
									cloneRepository(git, jobContext.getProjectGitDir().getAbsolutePath(), 
											cloneInfo.getCloneUrl(), jobContext.getCommitId().name(), 
											checkoutFacade.isWithLfs(), checkoutFacade.isWithSubmodules(),
											cloneDepth, ExecutorUtils.newInfoLogger(jobLogger), ExecutorUtils.newWarningLogger(jobLogger));
								} catch (Exception e) {
									jobLogger.error("Step \"" + stepNames + "\" is failed: " + getErrorMessage(e));
									return false;
								}
							} else {
								ServerSideFacade serverSideFacade = (ServerSideFacade) facade;
								try {
									serverSideFacade.execute(buildDir, new ServerSideFacade.Runner() {
										
										@Override
										public Map<String, byte[]> run(File inputDir, Map<String, String> placeholderValues) {
											return jobContext.runServerStep(position, inputDir, placeholderValues, jobLogger);
										}
										
									});
								} catch (Exception e) {
									jobLogger.error("Step \"" + stepNames + "\" is failed: " + getErrorMessage(e));
									return false;
								}
							}
							jobLogger.success("Step \"" + stepNames + "\" is successful");
							return true;
						}

						@Override
						public void skip(LeafFacade facade, List<Integer> position) {
							jobLogger.notice("Step \"" + entryFacade.getNamesAsString(position) + "\" is skipped");
						}
						
					}, new ArrayList<>());
		
					if (!successful)
						throw new FailedException();
				} finally {
					// Fix https://code.onedev.io/projects/160/issues/597
					if (SystemUtils.IS_OS_WINDOWS && workspaceDir.exists())
						FileUtils.deleteDir(workspaceDir);
					FileUtils.deleteDir(buildDir);
				}
			}
			
		}, jobContext.getResourceRequirements(), jobLogger);
	}

	@Override
	public void test(TestData testData, TaskLogger jobLogger) {
		OneDev.getInstance(ResourceManager.class).run(new Runnable() {

			@Override
			public void run() {
				Commandline git = new Commandline(AppLoader.getInstance(GitConfig.class).getExecutable());
				testCommands(git, testData.getCommands(), jobLogger);
			}
			
		}, new HashMap<>(), jobLogger);
		
	}
	
	@Editable(name="Specify Shell/Batch Commands to Run")
	public static class TestData implements Serializable {

		private static final long serialVersionUID = 1L;

		private List<String> commands = new ArrayList<>();

		@Editable
		@OmitName
		@Code(language=Code.SHELL)
		@Size(min=1, message="May not be empty")
		public List<String> getCommands() {
			return commands;
		}

		public void setCommands(List<String> commands) {
			this.commands = commands;
		}
		
	}

}