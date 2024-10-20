package com.sergistan.learnengbot.services;

import com.sergistan.learnengbot.exceptions.ServiceException;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.MaybeInaccessibleMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;

@Component
public class TelegramBotService extends TelegramLongPollingBot {
    @Autowired
    public TelegramBotService(@Value("${bot_token}") String botToken,
                              YandexTranslateApiService yandexTranslateApiService,
                              RandomEngWordApiService randomEngWordApiService,
                              LearningWordService learningWordService,
                              UserService userService) {
        super(botToken);
        this.yandexTranslateApiService = yandexTranslateApiService;
        this.randomEngWordApiService = randomEngWordApiService;
        this.learningWordService = learningWordService;
        this.userService = userService;
        registerCommands();
    }

    private void registerCommands() {
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "Получение стартового сообщения"));
        listOfCommands.add(new BotCommand("/random", "Получение случайного английского слова с переводом"));
        listOfCommands.add(new BotCommand("/practice", "Вывод сохраненных слов с переводом для повторения"));
        listOfCommands.add(new BotCommand("/help", "Информация как использовать этот бот"));
        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            throw new ServiceException("Ошибка отправки сообщения", e);
        }
    }

    private final YandexTranslateApiService yandexTranslateApiService;
    private final RandomEngWordApiService randomEngWordApiService;
    private final LearningWordService learningWordService;
    private final UserService userService;
    private static final String START = "/start";
    private static final String RANDOM = "/random";
    private static final String PRACTICE = "/practice";
    private static final String DELETE = "/delete";
    private static final String HELP = "/help";
    private static final String NOUN = "noun";
    private static final String VERB = "verb";
    private static final String ADJECTIVE = "adjective";
    private static final String ADVERB = "adverb";
    private static final String LEARN = "learn";
    private static final String MISS = "miss";

    @Override
    public String getBotUsername() {
        return "LearningEngFromSergistanBot";
    }

    @SneakyThrows
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasCallbackQuery()) {
            handleCallbackQuery(update);
        } else if (update.hasMessage() && update.getMessage().hasText()) {
            handleMessage(update);
        }
    }

    private void handleCallbackQuery(Update update) {
        String data = update.getCallbackQuery().getData();
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        MaybeInaccessibleMessage message = update.getCallbackQuery().getMessage();
        CallbackQuery callbackQuery = update.getCallbackQuery();
        int messageId = callbackQuery.getMessage().getMessageId();
        switch (data) {
            case NOUN, VERB, ADJECTIVE, ADVERB -> {
                randomCommand(chatId, data);
                deleteMessage(chatId, messageId);
            }
            case LEARN -> {
                learningCommand(message);
                deleteMessage(chatId, messageId);
            }
            case MISS -> {
                missingCommand(message);
                deleteMessage(chatId, messageId);
            }
        }
    }

    private void handleMessage(Update update) {
        String message = update.getMessage().getText();
        Long chatId = update.getMessage().getChatId();
        String userName = update.getMessage().getChat().getUserName();
        if (message.startsWith(DELETE)) {
            deleteCommand(chatId, userName, message);
        } else {
            switch (message) {
                case START -> startCommand(chatId, userName);
                case RANDOM -> choosePartOfSpeech(chatId);
                case PRACTICE -> practiceCommand(chatId, userName);
                case HELP -> helpCommand(chatId);
                default -> translateCommand(chatId, message);
            }
        }
    }

    public void startCommand(Long chatId, String userName) {
        userService.createUser(chatId, userName);
        sendMessage(chatId, String.format("""
                Добро пожаловать в бот для перевода и запоминания английских слов, %s!

                Здесь Вы сможете перевести русское слово на английское.

                Для этого просто напишите слово, которое хотите перевести.

                Дополнительные команды:
                /help - получение справки
                """, userName));
    }

    private void practiceCommand(Long chatId, String username) {
        String words = learningWordService.workoutWords(chatId, username);
        sendMessage(chatId, words);
    }

    public void helpCommand(Long chatId) {
        String helpText = """
                Справочная информация по боту:
                                
                Для получения случайного английского слова с переводом воспользуйтесь командой: /random.
                                
                Для повторения ранее добавленных английских слов с переводом воспользуйтесь командой: /practice.
                                
                Для получения перевода русского слова на английский язык просто напишите его в чате.
                                
                Для удаления слова из списка сохраненных слов:
                1) вариант: выполните команду /delete и через пробел после команды напишите английское слово, которое хотите удалить;
                2) вариант: напишите русское слово в чате и нажмите на кнопку "Не запоминать слово".
                """;
        sendMessage(chatId, helpText);
    }

    public void translateCommand(Long chatId, String message) {
        try {
            String translatedWord = yandexTranslateApiService.translateRuToEn(message);
            if (translatedWord.startsWith("Ваше введенное слово не подходит!")) {
                sendMessage(chatId, translatedWord);
            } else {
                sendMessageFromRandomAndChat(chatId, message, translatedWord);
            }
        } catch (Exception e) {
            throw new ServiceException("Ошибка перевода слова", e);
        }
    }

    private void deleteCommand(Long chatId, String userName, String message) {
        try {
            String textFromManualDeletingWord = "Некорректный ввод";
            String[] splitWords = message.split(" ");
            if (splitWords.length != 2) {
                sendMessage(chatId, textFromManualDeletingWord);
                return;
            }
            Optional<String> word = Arrays.stream(splitWords).skip(1).findFirst();
            if (word.isPresent()) {
                textFromManualDeletingWord = learningWordService.manualDeletingWord(chatId, userName, word.get());
                sendMessage(chatId, textFromManualDeletingWord);
            }
        } catch (Exception e) {
            throw new ServiceException("Ошибка удаления слова", e);
        }
    }

    public void randomCommand(Long chatId, String partOfSpeech) {
        try {
            String randomWord = randomEngWordApiService.getRandomEnglishWord(partOfSpeech);
            String translatedWord = yandexTranslateApiService.translateEnToRu(randomWord);
            sendMessageFromRandomAndChat(chatId, translatedWord, randomWord);
        } catch (Exception e) {
            throw new ServiceException("Ошибка при получении случайного слова", e);
        }
    }

    public void learningCommand(MaybeInaccessibleMessage message) {
        Long chatId = message.getChatId();
        String userName = ((Message) message).getChat().getUserName();
        String wordWithTranslate = ((Message) message).getText();
        learningWordService.saveWord(chatId, userName, wordWithTranslate);
        sendMessage(chatId, String.format("Английское слово с переводом: \"%s\" сохранено для повторения.", wordWithTranslate));
    }

    public void missingCommand(MaybeInaccessibleMessage message) {
        Long chatId = message.getChatId();
        String userName = ((Message) message).getChat().getUserName();
        String wordWithTranslate = ((Message) message).getText();
        learningWordService.deleteWordIfItWasAddedEarlier(chatId, userName, wordWithTranslate);
        sendMessage(chatId, "Слово удалено из списка для повторения.");
    }

    public void sendMessageFromRandomAndChat(Long chatId, String translatedWord, String word) {
        String messageText = String.format("%s - %s", translatedWord, word);
        SendMessage sendMessage = new SendMessage(String.valueOf(chatId), messageText);

        InlineKeyboardButton learnButton = createInlineKeyboardButton("Запомнить слово", LEARN);
        InlineKeyboardButton missButton = createInlineKeyboardButton("Не запоминать слово", MISS);

        List<InlineKeyboardButton> buttons = Arrays.asList(learnButton, missButton);
        List<List<InlineKeyboardButton>> buttonRows = Collections.singletonList(buttons);
        createAndSetInlineKeyboardMarkup(sendMessage, buttonRows);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new ServiceException("Ошибка отправки сообщения", e);
        }
    }

    public void choosePartOfSpeech(Long chatId) {
        SendMessage sendMessage = new SendMessage(String.valueOf(chatId), "Выберите часть речи:");

        InlineKeyboardButton nounButton = createInlineKeyboardButton("Существительное", NOUN);
        InlineKeyboardButton verbButton = createInlineKeyboardButton("Глагол", VERB);
        InlineKeyboardButton adjectiveButton = createInlineKeyboardButton("Прилагательное", ADJECTIVE);
        InlineKeyboardButton adverbButton = createInlineKeyboardButton("Наречие", ADVERB);

        List<InlineKeyboardButton> buttons1 = Arrays.asList(nounButton, verbButton);
        List<InlineKeyboardButton> buttons2 = Arrays.asList(adjectiveButton, adverbButton);
        List<List<InlineKeyboardButton>> buttonRows = Arrays.asList(buttons1, buttons2);
        createAndSetInlineKeyboardMarkup(sendMessage, buttonRows);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new ServiceException("Ошибка отправки сообщения", e);
        }
    }

    private InlineKeyboardButton createInlineKeyboardButton(String text, String callbackData) {
        InlineKeyboardButton missButton = new InlineKeyboardButton(text);
        missButton.setCallbackData(callbackData);
        return missButton;
    }

    private void createAndSetInlineKeyboardMarkup(SendMessage sendMessage, List<List<InlineKeyboardButton>> buttonRows) {
        InlineKeyboardMarkup markup = createInlineKeyboardMarkup(buttonRows);
        sendMessage.setReplyMarkup(markup);
    }

    private InlineKeyboardMarkup createInlineKeyboardMarkup(List<List<InlineKeyboardButton>> buttonRows) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();

        for (List<InlineKeyboardButton> buttons : buttonRows) {
            rowsInLine.add(buttons);
        }
        markup.setKeyboard(rowsInLine);
        return markup;
    }

    public void sendMessage(Long chatId, String text) {
        SendMessage sendMessage = new SendMessage(String.valueOf(chatId), text);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new ServiceException("Ошибка отправки сообщения", e);
        }
    }

    private void deleteMessage(Long chatId, int messageId) {
        DeleteMessage deleteMessage = new DeleteMessage(String.valueOf(chatId), messageId);
        try {
            execute(deleteMessage);
        } catch (TelegramApiException e) {
            throw new ServiceException("Ошибка удаления сообщения", e);
        }
    }
}
