package com.mitsui.fastgit.ui;

import com.mitsui.fastgit.dto.GitProjectDto;
import com.mitsui.fastgit.dto.ProjectCollectionDto;
import com.mitsui.fastgit.exception.ConfigFileFormatException;
import com.mitsui.fastgit.exception.ConfigFileNotExistException;
import com.mitsui.fastgit.service.GitConfigService;
import com.mitsui.fastgit.service.GitProjectService;
import com.mitsui.fastgit.service.GitService;
import com.mitsui.fastgit.service.ProjectCollectionService;
import com.mitsui.fastgit.service.impl.GitConfigServiceImpl;
import com.mitsui.fastgit.service.impl.GitProjectServiceImpl;
import com.mitsui.fastgit.service.impl.GitServiceImpl;
import com.mitsui.fastgit.service.impl.ProjectCollectionServiceImpl;
import com.mitsui.fastgit.util.CollectionUtil;
import com.mitsui.fastgit.util.DateUtil;
import com.mitsui.fastgit.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class MainForm {
    private static final Logger logger = LoggerFactory.getLogger(MainForm.class);
    private static final GitProjectService projectService = new GitProjectServiceImpl();
    private static final GitConfigService configService = new GitConfigServiceImpl();
    private static final ProjectCollectionService projectCollectionService = new ProjectCollectionServiceImpl();
    private static GitService gitService;
    public static final String OPERATE_TYPE_CREATE_BRANCH = "创建分支";
    public static final String OPERATE_TYPE_CREATE_TAG = "创建tag";
    public static final String OPERATE_TYPE_MERGE_BRANCH = "merge分支";
    public static final String OPERATE_TYPE_DELETE_BRANCH = "删除分支";
    public static final String OPERATE_TYPE_PUSH_BRANCH = "push分支";
    public static final String OPERATE_TYPE_DELETE_TAG = "删除tag";
    public static final String OPERATE_TYPE_CHECKOUT_BRANCH = "切换分支";
    public static final String OPERATE_TYPE_DELETE_OLD_BRANCH = "删除一个月之前的分支";
    private JFrame mainForm;
    private List<GitProjectDto> projectList;
    private List<String> selectedProjectList = new ArrayList();
    private String selectOperateOption = "创建分支";
    private JTextField createSourceBranchText;
    private JTextField createBranchText;
    private JTextField mergeSourceBranchText;
    private JTextField mergeTargetBranchText;
    private JTextField deleteBranchText;
    private JTextField deleteTagText;
    private JTextField pushBranchMsgText;
    private JButton confirmButton;
    private static final String CONFIRM_BUTTON_TEXT_ENABLE = "确认";
    private static final String PROJECT_COLLECTION_DEFAULT_VALUE = "";
    private final int DEFAULT_FORM_WIDTH = 500;
    private final int DEFAULT_FORM_HEIGHT = 500;
    private JCheckBox createBranchSkipExistProjectChkBox;
    private JCheckBox createSkipChangeVersionChkBox;

    private JTextField createTagSourceBranchText;
    private JTextField createTagText;
    private LoadingPanel loadingPanel = null;
    private JCheckBox mergeBranchIgnorePomChkBox;
    private JComboBox projectCollectionJBox;
    private List<ProjectCollectionDto> projectCollectionList;
    private List<JCheckBox> projectCheckBoxList = new ArrayList();
    private JLabel selectProjectCountLabel = new JLabel();
    private JTextField checkoutBranchText;
    private JCheckBox projectSelectAllCheckBox = this.createJCheckBox("全选");

    public MainForm() {
    }

    public static MainForm createForm() {
        MainForm mainForm = new MainForm();
        mainForm.init();
        return mainForm;
    }

    private static void initServiceObject() {
        gitService = new GitServiceImpl();
    }

    private void init() {
        try {
            boolean result = this.validateAndInitConfigFile();
            if (!result) {
                this.exit();
                return;
            }

            initServiceObject();
            this.initFormUi();
        } catch (Exception e) {
            logger.error("初始化main form 发生异常", e);
        }

    }

    private boolean validateAndInitConfigFile() {
        try {
            configService.validateConfigFile();
            return true;
        } catch (ConfigFileNotExistException e) {
            try {
                configService.copyConfigFile();
            } catch (IOException ioException) {
                logger.error("拷贝配置文件发生异常", ioException);
                this.showErrorMessage("拷贝配置文件失败,错误信息:" + ioException.getMessage());
                return false;
            }

            this.showErrorMessage("您尚未进行配置配置文件，已将默认配置文件拷贝到当前应用目录下，您可自行修改");
            logger.error("未找到配置文件", e);
            return true;
        } catch (ConfigFileFormatException e) {
            this.showErrorMessage("配置文件格式错误，错误信息:" + e.getMessage());
            logger.error("配置文件格式错误", e);
            return false;
        }
    }

    private void initFormUi() {
        this.initForm();
        this.initFormUiForProjectCollection();
        this.initFormUiForProject();
        this.initFormUiForOperateOption();
        this.initConfirmButton();
        this.initAppVersion();
        this.showForm();
    }

    private void initFormUiForProjectCollection() {
        JPanel projectCollectionPanel = new JPanel();
        projectCollectionPanel.setBorder(new EmptyBorder(3, 19, 10, 0));
        projectCollectionPanel.setLayout(new FlowLayout(0));
        this.mainForm.add(projectCollectionPanel);
        JLabel projectCollectionTipLabel = new JLabel("快速选择项目集:");
        projectCollectionPanel.add(projectCollectionTipLabel);
        this.projectCollectionJBox = new JComboBox();
        this.projectCollectionJBox.setPreferredSize(new Dimension(200, 30));
        this.projectCollectionJBox.setEditable(true);
        projectCollectionPanel.add(this.projectCollectionJBox);
        this.projectCollectionJBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                MainForm.this.projectCollectionSelectEvent(e);
            }
        });
        JButton saveProjectCollectionButton = new JButton("保存");
        projectCollectionPanel.add(saveProjectCollectionButton);
        saveProjectCollectionButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MainForm.this.saveProjectCollection();
            }
        });
        this.loadProjectCollection((String) null);
    }

    private void projectCollectionSelectEvent(ItemEvent e) {
        String selectCollectionName = e.getItem().toString();
        if (!StringUtil.isNullOrEmpty(selectCollectionName)) {
            if ("".equals(selectCollectionName)) {
                this.projectCheckBoxList.stream().forEach((t) -> {
                    t.setSelected(false);
                });
                this.projectSelectAllCheckBox.setSelected(false);
            } else if (e.getStateChange() == 1) {
                if (!CollectionUtil.isEmpty(this.projectList) && !StringUtil.isNullOrEmpty(selectCollectionName) && !CollectionUtil.isEmpty(this.projectCollectionList)) {
                    ProjectCollectionDto collectionDto = this.projectCollectionList.stream().filter((t) -> {
                        return t.getName().equals(selectCollectionName);
                    }).findFirst().orElse(null);
                    if (collectionDto == null) {
                        return;
                    }

                    List<String> collectionProjectList = collectionDto.getProjectList();
                    List<String> selectCheckProjectList = new ArrayList();
                    if (!CollectionUtil.isEmpty(collectionProjectList)) {
                        this.projectList.stream().forEach((t) -> {
                            if (collectionProjectList.contains(t.getName())) {
                                selectCheckProjectList.add(t.getName());
                            }

                        });
                    }

                    if (!CollectionUtil.isEmpty(this.projectCheckBoxList)) {
                        this.projectCheckBoxList.stream().forEach((t) -> {
                            if (selectCheckProjectList.contains(t.getText().trim())) {
                                t.setSelected(true);
                            } else {
                                t.setSelected(false);
                            }

                        });
                    }
                }

                if (!CollectionUtil.isEmpty(this.projectList) && !CollectionUtil.isEmpty(this.selectedProjectList) && this.projectList.size() == this.selectedProjectList.size()) {
                    this.projectSelectAllCheckBox.setSelected(true);
                } else {
                    this.projectSelectAllCheckBox.setSelected(false);
                }

            }
        }
    }

    private void loadProjectCollection(String select) {
        this.projectCollectionJBox.removeAllItems();
        this.projectCollectionJBox.addItem("");
        this.projectCollectionList = projectCollectionService.getList();
        if (!CollectionUtil.isEmpty(this.projectCollectionList)) {
            this.projectCollectionList.stream().forEach((e) -> {
                this.projectCollectionJBox.addItem(e.getName());
            });
            if (!StringUtil.isNullOrEmpty(select)) {
                this.projectCollectionJBox.setSelectedItem(select);
            }

        }
    }

    private void saveProjectCollection() {
        int result = this.showConfirmDialog();
        if (result == 0) {
            String selectCollectionName = this.projectCollectionJBox.getSelectedItem().toString();
            if (!StringUtil.isNullOrEmpty(selectCollectionName) && !selectCollectionName.equals("")) {
                if (CollectionUtil.isEmpty(this.projectCollectionList)) {
                    this.projectCollectionList = new ArrayList();
                }

                ProjectCollectionDto selectCollection = this.projectCollectionList.stream().filter((e) -> {
                    return e.getName().equals(selectCollectionName);
                }).findFirst().orElse(null);
                if (selectCollection == null) {
                    selectCollection = new ProjectCollectionDto();
                    selectCollection.setName(selectCollectionName);
                    selectCollection.setProjectList(this.selectedProjectList);
                    this.projectCollectionList.add(selectCollection);
                } else {
                    selectCollection.setProjectList(this.selectedProjectList);
                }

                projectCollectionService.save(this.projectCollectionList);
                this.loadProjectCollection(selectCollectionName);
                this.showInfoMessage("保存成功");
            } else {
                this.showWarnMessage("请填写自定义项目集合名称");
            }
        }
    }

    private void showForm() {
        this.mainForm.pack();
        this.mainForm.setVisible(true);
    }

    private void initForm() {
        this.mainForm = new JFrame("FastGit");
        this.mainForm.setMinimumSize(new Dimension(500, 500));
        this.mainForm.setLocationRelativeTo(null);
        BoxLayout layout = new BoxLayout(this.mainForm.getContentPane(), 1);
        this.mainForm.setLayout(layout);
        this.mainForm.setDefaultCloseOperation(3);
    }

    private void initFormUiForProject() {
        JPanel projectPanel = new JPanel();
        projectPanel.setBorder(new EmptyBorder(0, 20, 0, 0));
        projectPanel.setLayout(new GridLayout(0, 2));
        JLabel projectTipLabel = new JLabel("请选择需要操作的项目:");
        projectTipLabel.setBorder(new EmptyBorder(3, 0, 3, 0));
        projectPanel.add(projectTipLabel);
        projectPanel.add(new JLabel());
        this.projectSelectAllCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                JCheckBox jCheckBox = (JCheckBox) e.getSource();
                if (!CollectionUtil.isEmpty(MainForm.this.projectCheckBoxList)) {
                    if (jCheckBox.isSelected()) {
                        MainForm.this.projectCheckBoxList.forEach((c) -> {
                            c.setSelected(true);
                        });
                    } else {
                        MainForm.this.projectCheckBoxList.forEach((c) -> {
                            c.setSelected(false);
                        });
                    }

                }
            }
        });
        projectPanel.add(this.projectSelectAllCheckBox);
        this.selectProjectCountLabel.setForeground(Color.GRAY);
        projectPanel.add(this.selectProjectCountLabel);
        this.showSelectProjectCount();
        this.projectList = projectService.getList();
        this.initProjectGitObj();
        if (this.projectList != null && !this.projectList.isEmpty()) {
            this.projectList.stream().forEach((e) -> {
                JCheckBox projectCheckBox = this.createJCheckBox(e.getName());
                this.projectCheckBoxList.add(projectCheckBox);
                projectCheckBox.addItemListener(new ItemListener() {
                    @Override
                    public void itemStateChanged(ItemEvent e) {
                        MainForm.this.projectItemStateChanged(e);
                    }
                });
                projectPanel.add(projectCheckBox);
            });
            this.selectProjectCountLabel.setBorder(new EmptyBorder(8, 0, 0, 0));
            this.mainForm.add(projectPanel);
        } else {
            this.showErrorMessage("未获取到项目配置信息，请检查你的配置");
        }
    }

    private void initProjectGitObj() {
        if (!CollectionUtil.isEmpty(this.projectList)) {
            (new Thread(new Runnable() {
                @Override
                public void run() {
                    MainForm.logger.info("开始异步初始化项目git对象");
                    MainForm.this.projectList.forEach(pro -> {
                        if (pro != null) {
                            try {
                                pro.setGit(MainForm.gitService.getGit(pro));
                            } catch (Exception e) {
                                MainForm.logger.error("初始化{}项目git 对象发生异常", pro.getName(), e);
                            }
                        }
                    });
                    MainForm.logger.info("项目git对象异步初始化完成");
                }
            }, "init project obj")).start();
        }
    }

    private void showSelectProjectCount() {
        int selectCount = 0;
        int totalCount = 0;
        if (!CollectionUtil.isEmpty(this.selectedProjectList)) {
            selectCount = this.selectedProjectList.size();
        }

        if (!CollectionUtil.isEmpty(this.projectList)) {
            totalCount = this.projectList.size();
        }

        String text = selectCount + "/" + totalCount;
        this.selectProjectCountLabel.setText(text);
    }

    private JCheckBox createJCheckBox(String name) {
        JCheckBox checkBox = new JCheckBox(name);
        return checkBox;
    }

    private void initFormUiForOperateOption() {
        JPanel operateOptionPanel = new JPanel();
        operateOptionPanel.setBorder(new EmptyBorder(10, 0, 0, 0));
        operateOptionPanel.setLayout(new FlowLayout(0));
        this.mainForm.add(operateOptionPanel);
        JPanel selectOperateOptionPanel = new JPanel();
        selectOperateOptionPanel.setLayout(new FlowLayout(0));
        JLabel operateOptionTipLabel = new JLabel("请选择操作类型:");
        selectOperateOptionPanel.add(operateOptionTipLabel);
        String[] operateOptionArray = new String[]{"创建分支", "创建tag", "merge分支", "删除分支", "push分支", "删除tag", "切换分支", "删除一个月之前的分支"};
        JComboBox operateOptionComBox = new JComboBox(operateOptionArray);
        selectOperateOptionPanel.add(operateOptionComBox);
        operateOptionPanel.add(selectOperateOptionPanel);
        JPanel createBranchPanel = new JPanel();
        operateOptionPanel.add(createBranchPanel);
        JPanel createBranchPanelBranch = new JPanel();
        createBranchPanel.add(createBranchPanelBranch);
        JLabel createSourceBranchLabel = new JLabel("源分支/tag:");
        createBranchPanelBranch.add(createSourceBranchLabel);
        this.createSourceBranchText = this.createTextField();
        createBranchPanelBranch.add(this.createSourceBranchText);
        JLabel createBranchLabel = new JLabel("新建分支:");
        createBranchPanelBranch.add(createBranchLabel);
        this.createBranchText = this.createTextField();
        createBranchPanelBranch.add(this.createBranchText);
        JPanel createBranchSkipPanel = new JPanel();
        this.createBranchSkipExistProjectChkBox = this.createJCheckBox("跳过已创建分支项目");
        this.createSkipChangeVersionChkBox = this.createJCheckBox("跳过修改pom版本");
        createBranchSkipPanel.add(this.createBranchSkipExistProjectChkBox);
        createBranchSkipPanel.add(this.createSkipChangeVersionChkBox);
        createBranchPanel.add(createBranchSkipPanel);
        final JPanel createTagPanel = new JPanel();
        createTagPanel.setVisible(false);
        operateOptionPanel.add(createTagPanel);
        JPanel createTagPanelBranch = new JPanel();
        createTagPanel.add(createTagPanelBranch);
        JLabel createTagSourceBranchLabel = new JLabel("源分支:");
        createTagPanelBranch.add(createTagSourceBranchLabel);
        this.createTagSourceBranchText = this.createTextField();
        createTagPanelBranch.add(this.createTagSourceBranchText);
        JLabel createTagLabel = new JLabel("tag名称:");
        createTagPanelBranch.add(createTagLabel);
        this.createTagText = this.createTextField();
        createTagPanelBranch.add(this.createTagText);
        final JPanel mergePanel = new JPanel();
        mergePanel.setVisible(false);
        operateOptionPanel.add(mergePanel);
        JLabel mergePanelSourceLabel = new JLabel("源分支/tag:");
        mergePanel.add(mergePanelSourceLabel);
        this.mergeSourceBranchText = this.createTextField();
        mergePanel.add(this.mergeSourceBranchText);
        JLabel mergeTargetLabel = new JLabel("目标分支:");
        mergePanel.add(mergeTargetLabel);
        this.mergeTargetBranchText = this.createTextField();
        mergePanel.add(this.mergeTargetBranchText);
        JPanel mergeBranchIgnorePomPanel = new JPanel();
        this.mergeBranchIgnorePomChkBox = this.createJCheckBox("忽略pom文件");
        mergeBranchIgnorePomPanel.add(this.mergeBranchIgnorePomChkBox);
        mergePanel.add(mergeBranchIgnorePomPanel);
        JPanel deleteBranchPanel = new JPanel();
        deleteBranchPanel.setVisible(false);
        operateOptionPanel.add(deleteBranchPanel);
        JLabel deleteBranchLabel = new JLabel("删除分支:");
        deleteBranchPanel.add(deleteBranchLabel);
        this.deleteBranchText = this.createTextField();
        deleteBranchPanel.add(this.deleteBranchText);
        final JPanel pushBranchPanel = new JPanel();
        pushBranchPanel.setVisible(false);
        operateOptionPanel.add(pushBranchPanel);
        JLabel pushBranchLabel = new JLabel("commit msg:");
        pushBranchPanel.add(pushBranchLabel);
        this.pushBranchMsgText = this.createTextField(200);
        pushBranchPanel.add(this.pushBranchMsgText);
        final JPanel deleteTagPanel = new JPanel();
        deleteTagPanel.setVisible(false);
        operateOptionPanel.add(deleteTagPanel);
        JLabel deleteTagLabel = new JLabel("删除tag:");
        deleteTagPanel.add(deleteTagLabel);
        this.deleteTagText = this.createTextField();
        deleteTagPanel.add(this.deleteTagText);
        final JPanel checkoutBranchPanel = new JPanel();
        checkoutBranchPanel.setVisible(false);
        operateOptionPanel.add(checkoutBranchPanel);
        JLabel checkoutBranchLabel = new JLabel("切换分支:");
        checkoutBranchPanel.add(checkoutBranchLabel);
        this.checkoutBranchText = this.createTextField();
        checkoutBranchPanel.add(this.checkoutBranchText);
        operateOptionComBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == 1) {
                    if (e.getItem().equals("创建分支")) {
                        createBranchPanel.setVisible(true);
                        mergePanel.setVisible(false);
                        deleteBranchPanel.setVisible(false);
                        pushBranchPanel.setVisible(false);
                        createTagPanel.setVisible(false);
                        deleteTagPanel.setVisible(false);
                        checkoutBranchPanel.setVisible(false);
                        MainForm.this.selectOperateOption = "创建分支";
                    } else if (e.getItem().equals("merge分支")) {
                        createBranchPanel.setVisible(false);
                        mergePanel.setVisible(true);
                        deleteBranchPanel.setVisible(false);
                        pushBranchPanel.setVisible(false);
                        createTagPanel.setVisible(false);
                        deleteTagPanel.setVisible(false);
                        checkoutBranchPanel.setVisible(false);
                        MainForm.this.selectOperateOption = "merge分支";
                    } else if (e.getItem().equals("删除分支")) {
                        deleteBranchPanel.setVisible(true);
                        mergePanel.setVisible(false);
                        createBranchPanel.setVisible(false);
                        pushBranchPanel.setVisible(false);
                        createTagPanel.setVisible(false);
                        deleteTagPanel.setVisible(false);
                        checkoutBranchPanel.setVisible(false);
                        MainForm.this.selectOperateOption = "删除分支";
                    } else if (e.getItem().equals("push分支")) {
                        deleteBranchPanel.setVisible(false);
                        mergePanel.setVisible(false);
                        createBranchPanel.setVisible(false);
                        pushBranchPanel.setVisible(true);
                        createTagPanel.setVisible(false);
                        deleteTagPanel.setVisible(false);
                        checkoutBranchPanel.setVisible(false);
                        MainForm.this.selectOperateOption = "push分支";
                    } else if (e.getItem().equals("创建tag")) {
                        deleteBranchPanel.setVisible(false);
                        mergePanel.setVisible(false);
                        createBranchPanel.setVisible(false);
                        pushBranchPanel.setVisible(false);
                        createTagPanel.setVisible(true);
                        deleteTagPanel.setVisible(false);
                        checkoutBranchPanel.setVisible(false);
                        MainForm.this.selectOperateOption = "创建tag";
                    } else if (e.getItem().equals("删除tag")) {
                        deleteBranchPanel.setVisible(false);
                        mergePanel.setVisible(false);
                        createBranchPanel.setVisible(false);
                        pushBranchPanel.setVisible(false);
                        createTagPanel.setVisible(false);
                        deleteTagPanel.setVisible(true);
                        checkoutBranchPanel.setVisible(false);
                        MainForm.this.selectOperateOption = "删除tag";
                    } else if (e.getItem().equals("切换分支")) {
                        deleteBranchPanel.setVisible(false);
                        mergePanel.setVisible(false);
                        createBranchPanel.setVisible(false);
                        pushBranchPanel.setVisible(false);
                        createTagPanel.setVisible(false);
                        deleteTagPanel.setVisible(false);
                        checkoutBranchPanel.setVisible(true);
                        MainForm.this.selectOperateOption = "切换分支";
                    } else if (e.getItem().equals("删除一个月之前的分支")) {
                        deleteBranchPanel.setVisible(false);
                        mergePanel.setVisible(false);
                        createBranchPanel.setVisible(false);
                        pushBranchPanel.setVisible(false);
                        createTagPanel.setVisible(false);
                        deleteTagPanel.setVisible(false);
                        checkoutBranchPanel.setVisible(false);
                        MainForm.this.selectOperateOption = "删除一个月之前的分支";
                    }
                }

            }
        });
    }

    private JTextField createTextField() {
        return this.createTextField(100, 30);
    }

    private JTextField createTextField(int width, int height) {
        JTextField textField = new JTextField();
        textField.setPreferredSize(new Dimension(width, height));
        return textField;
    }

    private JTextField createTextField(int width) {
        return this.createTextField(width, 30);
    }

    private void initConfirmButton() {
        JPanel confirmPanel = new JPanel();
        confirmPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
        this.mainForm.add(confirmPanel);
        this.confirmButton = new JButton("确认");
        confirmPanel.add(this.confirmButton);
        this.confirmButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MainForm.this.confirmButtonClick();
            }
        });
    }

    private void confirmButtonClick() {
        int result = this.showConfirmDialog();
        if (result == 0) {
            this.confirmButtonClickForYes();
        }

    }

    private void confirmButtonClickForYes() {
        try {
            try {
                if (CollectionUtil.isEmpty(this.selectedProjectList)) {
                    this.showWarnMessage("请选择需要操作的项目");
                    return;
                }

                if (StringUtil.isNullOrEmpty(this.selectOperateOption)) {
                    this.showWarnMessage("请选择需要操作的类型");
                    return;
                }

                List<GitProjectDto> selectProject = this.projectList.stream().filter((e) -> {
                    return this.selectedProjectList.contains(e.getName());
                }).collect(Collectors.toList());
                this.dealBranchAsync(this.selectOperateOption, selectProject);
            } catch (Exception e) {
                logger.error("操作发生未知异常", e);
                this.showErrorMessage("操作发生未知异常");
            }

        } finally {

        }
    }

    private void dealBranchAsync(final String operateType, final List<GitProjectDto> projectList) {
        this.showLoading();
        SwingWorker<String, Object> task = new SwingWorker<String, Object>() {
            @Override
            protected String doInBackground() {
                MainForm.this.dealBranch(operateType, projectList);
                return "";
            }

            @Override
            protected void done() {
                MainForm.this.closeLoading();
            }
        };
        task.execute();
    }

    private void dealBranch(String operateType, List<GitProjectDto> projectList) {
        byte b = -1;
        switch (operateType.hashCode()) {
            case -1338895621:
                if (operateType.equals("创建tag")) {
                    b = 1;
                }
                break;
            case -976247677:
                if (operateType.equals("push分支")) {
                    b = 4;
                }
                break;
            case -911662890:
                if (operateType.equals("删除tag")) {
                    b = 5;
                }
                break;
            case -906423375:
                if (operateType.equals("删除一个月之前的分支")) {
                    b = 7;
                }
                break;
            case 650219624:
                if (operateType.equals("创建分支")) {
                    b = 0;
                }
                break;
            case 650707812:
                if (operateType.equals("切换分支")) {
                    b = 6;
                }
                break;
            case 664001325:
                if (operateType.equals("删除分支")) {
                    b = 3;
                }
                break;
            case 954321441:
                if (operateType.equals("merge分支")) {
                    b = 2;
                }
        }

        switch (b) {
            case 0:
                this.createBranch(projectList);
                break;
            case 1:
                this.createTag(projectList);
                break;
            case 2:
                this.mergeBranch(projectList);
                break;
            case 3:
                this.deleteBranch(projectList);
                break;
            case 4:
                this.pushBranch(projectList);
                break;
            case 5:
                this.deleteTag(projectList);
                break;
            case 6:
                this.checkoutBranch(projectList);
                break;
            case 7:
                this.deleteOldBranch(projectList);
        }

    }

    private void deleteOldBranch(List<GitProjectDto> selectProjectList) {
        try {
            Date now = new Date();
            Date date = DateUtil.addMonths(now, -1);
            gitService.deleteOldBranch(selectProjectList, date);
        } catch (Exception e) {
            logger.error("删除旧分支失败,", e);
            this.showErrorMessage("删除旧分支失败:" + e.getMessage());
            return;
        }

        this.showInfoMessage("删除旧分支成功");
    }

    private void checkoutBranch(List<GitProjectDto> selectProjectList) {
        try {
            String checkoutBranchName = this.checkoutBranchText.getText().trim();
            if (StringUtil.isNullOrEmpty(checkoutBranchName)) {
                this.showWarnMessage("请填写需要切换的分支");
                return;
            }

            gitService.checkoutBranch(selectProjectList, checkoutBranchName);
        } catch (Exception e) {
            logger.error("切换分支失败,", e);
            this.showErrorMessage("切换分支失败:" + e.getMessage());
            return;
        }

        this.showInfoMessage("分支切换成功");
    }

    private void createBranch(List<GitProjectDto> selectProjectList) {
        try {
            String createBranch = this.createBranchText.getText().trim();
            String createSourceBranch = this.createSourceBranchText.getText().trim();
            boolean isSkipExistProject = this.createBranchSkipExistProjectChkBox.isSelected();
            boolean isSkipChangeVersion = this.createSkipChangeVersionChkBox.isSelected();
            if (StringUtil.isNullOrEmpty(createSourceBranch)) {
                this.showWarnMessage("请填创建源分支");
                return;
            }

            if (StringUtil.isNullOrEmpty(createBranch)) {
                this.showWarnMessage("请填写要创建的分支");
                return;
            }

            gitService.createBranch(selectProjectList, createSourceBranch, createBranch, isSkipExistProject, isSkipChangeVersion);
        } catch (Exception e) {
            logger.error("创建分支失败,", e);
            this.showErrorMessage("创建分支失败:" + e.getMessage());
            return;
        }

        this.showInfoMessage("分支创建成功");
    }

    private void createTag(List<GitProjectDto> selectProjectList) {
        try {
            String createTag = this.createTagText.getText().trim();
            String createTagSourceBranch = this.createTagSourceBranchText.getText().trim();
            if (StringUtil.isNullOrEmpty(createTagSourceBranch)) {
                this.showWarnMessage("请填创建源分支");
                return;
            }

            if (StringUtil.isNullOrEmpty(createTag)) {
                this.showWarnMessage("请填写要创建的tag名称");
                return;
            }

            gitService.createTag(selectProjectList, createTagSourceBranch, createTag);
        } catch (Exception e) {
            logger.error("创建tag失败,", e);
            this.showErrorMessage("创建tag失败:" + e.getMessage());
            return;
        }

        this.showInfoMessage("tag创建成功");
    }

    private void mergeBranch(List<GitProjectDto> selectProjectList) {
        String targetBranch;
        try {
            String sourceBranch = this.mergeSourceBranchText.getText().trim();
            targetBranch = this.mergeTargetBranchText.getText().trim();
            if (StringUtil.isNullOrEmpty(sourceBranch)) {
                this.showWarnMessage("请填需要merge的源分支");
                return;
            }

            if (StringUtil.isNullOrEmpty(targetBranch)) {
                this.showWarnMessage("请填需要merge的目标分支");
                return;
            }

            boolean isIgnorePom = this.mergeBranchIgnorePomChkBox.isSelected();
            List<String> ignoreFileList = new ArrayList();
            if (isIgnorePom) {
                ignoreFileList.add("pom.xml");
            }

            gitService.merge(selectProjectList, sourceBranch, targetBranch, ignoreFileList);
        } catch (Exception e) {
            targetBranch = "merge分支完成,异常信息:" + e.getMessage();
            logger.error(targetBranch, e);
            this.showErrorMessage(targetBranch);
            return;
        }

        this.showInfoMessage("merge分支成功");
    }

    private void deleteBranch(List<GitProjectDto> selectProjectList) {
        try {
            String deleteBranch = this.deleteBranchText.getText().trim();
            if (StringUtil.isNullOrEmpty(deleteBranch)) {
                this.showWarnMessage("请填待删除的分支");
                return;
            }

            gitService.deleteBranch(selectProjectList, deleteBranch);
        } catch (Exception e) {
            logger.error("删除分支失败,", e);
            this.showErrorMessage("删除分支失败:" + e.getMessage());
            return;
        }

        this.showInfoMessage("分支删除成功");
    }

    private void deleteTag(List<GitProjectDto> selectProjectList) {
        try {
            String deleteTag = this.deleteTagText.getText().trim();
            if (StringUtil.isNullOrEmpty(deleteTag)) {
                this.showWarnMessage("请填待删除的tag");
                return;
            }

            gitService.deleteTag(selectProjectList, deleteTag);
        } catch (Exception e) {
            logger.error("删除tag失败,", e);
            this.showErrorMessage("删除tag失败:" + e.getMessage());
            return;
        }

        this.showInfoMessage("tag删除成功");
    }

    private void pushBranch(List<GitProjectDto> selectProjectList) {
        try {
            String pushMsg = this.pushBranchMsgText.getText().trim();
            if (StringUtil.isNullOrEmpty(pushMsg)) {
                this.showWarnMessage("请填写commit msg");
                return;
            }

            gitService.push(selectProjectList, pushMsg);
        } catch (Exception e) {
            logger.error("提交分支失败,", e);
            this.showErrorMessage("提交分支失败:" + e.getMessage());
            return;
        }

        this.showInfoMessage("提交分支成功");
    }

    private int showConfirmDialog() {
        String title = "操作提示";
        String message = "您确认要操作吗?";
        return this.showConfirmDialog(title, message);
    }

    private int showConfirmDialog(String title, String message) {
        return JOptionPane.showConfirmDialog((Component) null, message, title, 0);
    }

    private void showWarnMessage(String message) {
        String title = " 操作提示";
        this.showMessage(title, message, 2);
    }

    private void showInfoMessage(String message) {
        String title = " 操作提示";
        this.showMessage(title, message, 1);
    }

    private void showErrorMessage(String message) {
        String title = "错误提示";
        this.showMessage(title, message, 0);
    }

    private void showMessage(String title, String message, int messageType) {
        JOptionPane.showMessageDialog((Component) null, message, title, messageType);
    }

    private void projectItemStateChanged(ItemEvent e) {
        JCheckBox jCheckBox = (JCheckBox) e.getSource();
        String name = jCheckBox.getText().trim();
        if (jCheckBox.isSelected()) {
            this.selectedProjectList.add(name);
        } else {
            this.selectedProjectList.remove(name);
        }

        this.showSelectProjectCount();
    }

    private void initAppVersion() {
        JPanel appVersionPanel = new JPanel();
        appVersionPanel.setBorder(new EmptyBorder(0, 0, 5, 0));
        this.mainForm.add(appVersionPanel);
        JLabel appVersionLabel = new JLabel();
        appVersionLabel.setText("当前应用版本号:1.0.0");
        appVersionPanel.add(appVersionLabel);
    }

    private void exit() {
        System.exit(1);
    }

    private void showLoading() {
        this.loadingPanel = new LoadingPanel();
        int width = this.mainForm.getWidth();
        int height = this.mainForm.getHeight();
        this.loadingPanel.setBounds(0, 0, width, height);
        this.mainForm.setGlassPane(this.loadingPanel);
        this.loadingPanel.setText("");
        this.loadingPanel.start();
    }

    private void closeLoading() {
        this.loadingPanel.interrupt();
    }
}
