def call(String message) {
  mail to: 'kps4k@yandex.ru',
      subject: "Status of pipeline: ${currentBuild.fullDisplayName}",
      body: "$message: Job ${env.JOB_NAME} build ${env.BUILD_NUMBER}\n More info at: ${env.BUILD_URL}"
}
